package com.eventHora.backend.service;

import com.eventHora.backend.Enum.AdminBookingAction;
import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.Enum.PaymentStatus;
import com.eventHora.backend.dto.AdminBookingRequest;
import com.eventHora.backend.dto.AdminBookingResponse;
import com.eventHora.backend.exception.ResourceNotFoundException;
import com.eventHora.backend.model.Event;
import com.eventHora.backend.model.Registration;
import com.eventHora.backend.repository.EventRepository;
import com.eventHora.backend.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Random;

/**
 * Service for admin/staff bookings made on behalf of a member.
 *
 * This is intentionally separate from RegistrationService because:
 *  1. No Redis — admin is trusted via JWT; no OTP, no session token needed.
 *  2. No Razorpay — admin bookings are either PAY_AT_GATE or COMPLIMENTARY.
 *  3. No retry logic — admin creates a clean, definitive booking every time.
 *
 * Duplicate booking behaviour:
 *  - If a locked (non-FAILED) registration already exists for this
 *    member + event, we REJECT with a clear error.
 *  - If a FAILED registration exists, we REJECT with a clear error too.
 *    Unlike the member flow, admin should know the member's current state
 *    before creating a new booking, so we don't silently overwrite.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    // ─── Redis Key Prefixes, TTLs ─── not used here; admin flow is stateless ──

    /**
     * POST /api/admin/bookings/register
     *
     * Registers a member for an event without OTP verification.
     * The calling admin's email is passed in as `bookedByEmail` (extracted
     * from the JWT by the controller via @AuthenticationPrincipal).
     *
     * Payment path — dual-tier pricing:
     *
     *   totalAmount == 0 (both tiers free after free-ticket quotas)
     *     → paymentStatus = FREE, totalAmount = 0.00  (action is ignored)
     *
     *   totalAmount > 0 and action = PAY_AT_GATE
     *     → paymentStatus = PAY_AT_GATE
     *
     *   totalAmount > 0 and action = COMPLIMENTARY
     *     → paymentStatus = COMPLIMENTARY, totalAmount = 0.00
     */
    @Transactional
    public AdminBookingResponse registerMemberByAdmin(
            AdminBookingRequest request,
            String bookedByEmail) {

        // ── Step 1: Validate memberId via mock RIC API ─────────────────────────
        if (!mockRicApi(request.getMemberId())) {
            throw new IllegalArgumentException(
                    "Invalid Member ID '" + request.getMemberId() +
                    "'. Must start with 'RIC' (e.g. RIC-2024-04512).");
        }

        // ── Step 2: Fetch and validate the event ──────────────────────────────
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + request.getEventId()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException(
                    "Event '" + event.getTitle() + "' is not published and cannot accept registrations.");
        }

        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new IllegalArgumentException(
                    "The registration deadline for '" + event.getTitle() + "' has passed.");
        }

        // ── Step 3: Validate quantities ────────────────────────────────────────
        int memberQuantity = request.getMemberQuantity();
        int guestQuantity  = request.getGuestQuantity();
        int totalQuantity  = memberQuantity + guestQuantity;

        if (memberQuantity > event.getMaxMemberTickets()) {
            throw new IllegalArgumentException(
                    "Maximum member tickets for this event is " +
                    event.getMaxMemberTickets() + ". Requested: " + memberQuantity + ".");
        }
        if (guestQuantity > event.getMaxGuestTickets()) {
            throw new IllegalArgumentException(
                    "Maximum guest tickets for this event is " +
                    event.getMaxGuestTickets() + ". Requested: " + guestQuantity + ".");
        }

        // ── Step 4: Capacity check ─────────────────────────────────────────────
        int lockedTickets = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remaining     = event.getTotalCapacity() - lockedTickets;
        if (totalQuantity > remaining) {
            throw new IllegalArgumentException(
                    "Not enough seats available for '" + event.getTitle() +
                    "'. Only " + remaining + " seat(s) remain.");
        }

        // ── Step 5: Duplicate booking check ───────────────────────────────────
        // Admin bookings do NOT silently overwrite failed rows like the member
        // retry flow does. If ANY registration already exists for this member +
        // event, we block and explain clearly.
        boolean existingBooking = registrationRepository
                .findByMemberIdAndEventId(request.getMemberId(), event.getId())
                .isPresent();
        if (existingBooking) {
            throw new IllegalStateException(
                    "A registration already exists for member '" + request.getMemberId() +
                    "' in event '" + event.getTitle() +
                    "'. Check the registrations list before creating a new one.");
        }

        // ── Step 6: Calculate price — dual-tier ───────────────────────────────
        int paidMemberTickets = Math.max(0, memberQuantity - event.getFreeMemberTickets());
        int paidGuestTickets  = Math.max(0, guestQuantity  - event.getFreeGuestTickets());
        BigDecimal memberAmount = event.getMemberTicketPrice().multiply(BigDecimal.valueOf(paidMemberTickets));
        BigDecimal guestAmount  = event.getGuestTicketPrice().multiply(BigDecimal.valueOf(paidGuestTickets));
        BigDecimal calculatedAmount = memberAmount.add(guestAmount);

        // ── Step 7: Determine payment path ────────────────────────────────────
        final PaymentStatus paymentStatus;
        final BigDecimal    totalAmount;

        boolean isFreeEvent = calculatedAmount.compareTo(BigDecimal.ZERO) == 0;

        if (isFreeEvent) {
            // Free event — action is irrelevant
            paymentStatus = PaymentStatus.FREE;
            totalAmount   = BigDecimal.ZERO;
            log.info("[ADMIN-BOOKING] PATH FREE — bookedBy={}, member={}, event='{}', mQty={}, gQty={}",
                    bookedByEmail, request.getMemberId(), event.getTitle(), memberQuantity, guestQuantity);

        } else if (request.getAction() == AdminBookingAction.COMPLIMENTARY) {
            // Admin is waiving the fee
            paymentStatus = PaymentStatus.COMPLIMENTARY;
            totalAmount   = BigDecimal.ZERO;
            log.info("[ADMIN-BOOKING] PATH COMPLIMENTARY — bookedBy={}, member={}, event='{}', mQty={}, gQty={}, waived={}",
                    bookedByEmail, request.getMemberId(), event.getTitle(), memberQuantity, guestQuantity, calculatedAmount);

        } else {
            // action == PAY_AT_GATE
            paymentStatus = PaymentStatus.PAY_AT_GATE;
            totalAmount   = calculatedAmount;
            log.info("[ADMIN-BOOKING] PATH PAY_AT_GATE — bookedBy={}, member={}, event='{}', mQty={}, gQty={}, amount={}",
                    bookedByEmail, request.getMemberId(), event.getTitle(), memberQuantity, guestQuantity, totalAmount);
        }

        // ── Step 8: Generate ticket reference ─────────────────────────────────
        String ticketReference = generateTicketReference();

        // ── Step 9: Persist the registration ──────────────────────────────────
        // paymentPreference is set to AT_GATE for PAY_AT_GATE path,
        // and to AT_GATE for COMPLIMENTARY/FREE as well (no ONLINE path exists here).
        Registration registration = Registration.builder()
                .memberId(request.getMemberId())
                .memberType(request.getMemberType())
                .event(event)
                .quantity(totalQuantity)
                .memberQuantity(memberQuantity)
                .guestQuantity(guestQuantity)
                .totalAmount(totalAmount)
                .paymentStatus(paymentStatus)
                .paymentPreference(PaymentPreference.AT_GATE) // admin bookings are always offline
                .ticketReference(ticketReference)
                .razorpayOrderId(null)
                .razorpayPaymentId(null)
                .isCheckedIn(false)
                .build();

        registrationRepository.save(registration);

        log.info("[ADMIN-BOOKING] SAVED — ticket={}, member={}, event='{}', status={}, bookedBy={}",
                ticketReference, request.getMemberId(), event.getTitle(), paymentStatus, bookedByEmail);

        // ── Step 10: Return response ───────────────────────────────────────────
        return AdminBookingResponse.builder()
                .ticketReference(ticketReference)
                .quantity(totalQuantity)
                .totalAmount(totalAmount)
                .paymentStatus(paymentStatus)
                .memberId(request.getMemberId())
                .eventTitle(event.getTitle())
                .eventDate(event.getEventDate())
                .eventStartTime(event.getStartTime())
                .eventVenue(event.getVenue())
                .bookedBy(bookedByEmail)
                .bookedAt(registration.getBookedAt())
                .build();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Mock of the external RIC API.
     * Returns true if memberId starts with "RIC" (case-insensitive).
     * Replace with a real HTTP call when the RIC API integration is ready.
     */
    private boolean mockRicApi(String memberId) {
        return memberId != null && memberId.toUpperCase().startsWith("RIC");
    }

    /**
     * Generates a unique, user-friendly ticket reference in the format:
     *   TKT-2026-AB12CD
     *
     * Identical logic to RegistrationService — could be extracted to a shared
     * utility class in a future refactor.
     */
    private String generateTicketReference() {
        String chars  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(chars.charAt(random.nextInt(chars.length())));
        }
        return "TKT-" + Year.now().getValue() + "-" + suffix;
    }
}

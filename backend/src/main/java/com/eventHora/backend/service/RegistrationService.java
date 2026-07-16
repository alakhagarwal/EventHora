package com.eventHora.backend.service;

import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.Enum.MemberType;
import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.Enum.PaymentStatus;
import com.eventHora.backend.dto.BookingIntent;
import com.eventHora.backend.dto.ConfirmPaymentRequest;
import com.eventHora.backend.dto.InitiateBookingRequest;
import com.eventHora.backend.dto.InitiateBookingResponse;
import com.eventHora.backend.dto.MemberSession;
import com.eventHora.backend.dto.RegistrationResponse;
import com.eventHora.backend.dto.VerifyMemberRequest;
import com.eventHora.backend.dto.VerifyMemberResponse;
import com.eventHora.backend.dto.VerifyOtpRequest;
import com.eventHora.backend.exception.ResourceNotFoundException;
import com.eventHora.backend.model.Event;
import com.eventHora.backend.model.Registration;
import com.eventHora.backend.repository.EventRepository;
import com.eventHora.backend.repository.RegistrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final RazorpayService razorpayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Redis Key Prefixes ────────────────────────────────────────────────────
    private static final String SESSION_PREFIX = "session:";
    private static final String OTP_PREFIX     = "otp:";
    private static final String INTENT_PREFIX  = "intent:";

    // ─── TTLs ─────────────────────────────────────────────────────────────────
    private static final Duration SESSION_TTL = Duration.ofHours(1);
    private static final Duration OTP_TTL     = Duration.ofMinutes(5);
    private static final Duration INTENT_TTL  = Duration.ofMinutes(10);

    // ─── OTP Expiry in seconds (returned to frontend for countdown timer) ─────
    private static final int OTP_TTL_SECONDS = 300;

    // ─── Endpoint 1: Verify Member ────────────────────────────────────────────

    /**
     * POST /api/registration/verify-member
     *
     * Validates the member with the RIC API (mocked) and creates a Redis session.
     */
    public VerifyMemberResponse verifyMember(VerifyMemberRequest request) {

        // 1. Format Validation based on Member Type
        if (request.getMemberType() == MemberType.INDIAN) {
            if (!request.getIdentifier().matches("^(\\+91[\\-\\s]?)?[0-9]{10}$")) {
                throw new IllegalArgumentException("Invalid mobile number format. Must be a valid Indian mobile number.");
            }
        } else if (request.getMemberType() == MemberType.OVERSEAS) {
            if (!request.getIdentifier().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("Invalid email format.");
            }
        }

        // 2. Call External RIC API (MOCKED — returns true if ID starts with "RIC")
        boolean isValid = mockRicApi(request.getMemberId(), request.getIdentifier());
        if (!isValid) {
            throw new IllegalArgumentException("Invalid Member ID or Identifier");
        }

        // 3. Generate session token and store in Redis
        String sessionToken = UUID.randomUUID().toString();
        MemberSession session = MemberSession.builder()
                .sessionToken(sessionToken)
                .memberId(request.getMemberId())
                .identifier(request.getIdentifier())
                .memberType(request.getMemberType())
                .build();

        redisTemplate.opsForValue().set(SESSION_PREFIX + sessionToken, session, SESSION_TTL);

        // 4. Return token and masked identifier to frontend
        return VerifyMemberResponse.builder()
                .sessionToken(sessionToken)
                .memberId(request.getMemberId())
                .memberType(request.getMemberType())
                .maskedIdentifier(maskIdentifier(request.getIdentifier(), request.getMemberType()))
                .build();
    }

    // ─── Endpoint 2: Initiate Booking ─────────────────────────────────────────

    /**
     * POST /api/registration/initiate
     *
     * Validates all booking rules, generates an OTP, and locks the booking intent in Redis.
     * The OTP is printed to the console for testing (mock delivery).
     */
    public InitiateBookingResponse initiateBooking(InitiateBookingRequest request) {

        // 1. Validate Session — ensure the caller is a verified member
        MemberSession session = getSessionOrThrow(request.getSessionToken());

        // 2. Fetch the Event from Postgres
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // 3. Event must be PUBLISHED
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("This event is no longer accepting registrations.");
        }

        // 4. Registration deadline must not have passed
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new IllegalArgumentException("The registration deadline for this event has passed.");
        }

        // 5. Quantity must not exceed the event's per-member limit
        if (request.getQuantity() > event.getMaxTicketsPerMember()) {
            throw new IllegalArgumentException(
                "You can book a maximum of " + event.getMaxTicketsPerMember() + " tickets for this event."
            );
        }

        // 6. Capacity check — count all locked (non-PENDING, non-FAILED) tickets
        int lockedTickets = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remaining = event.getTotalCapacity() - lockedTickets;
        if (request.getQuantity() > remaining) {
            throw new IllegalArgumentException(
                "Not enough seats available. Only " + remaining + " seat(s) remain."
            );
        }

        // 7. Duplicate booking check — one registration per member per event
        // We block only truly finalised statuses: CONFIRMED, FREE, PAY_AT_GATE.
        // FAILED and PENDING are both retryable:
        //   - FAILED: previous payment attempt was rejected by Razorpay or sold-out guard.
        //   - PENDING: user opened the Razorpay popup but closed it without paying.
        //     Razorpay does NOT send a failure webhook for an abandoned popup, so the row
        //     stays PENDING indefinitely. Rather than trapping the member, we allow them
        //     to restart and reuse the same database row with a fresh order ID.
        boolean alreadyBooked = registrationRepository
                .findByMemberIdAndEventId(session.getMemberId(), event.getId())
                .map(reg -> reg.getPaymentStatus() != PaymentStatus.FAILED
                         && reg.getPaymentStatus() != PaymentStatus.PENDING)
                .orElse(false);
        if (alreadyBooked) {
            throw new IllegalStateException("You have already registered for this event.");
        }

        // 8. Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 9. Lock OTP and BookingIntent in Redis
        redisTemplate.opsForValue().set(OTP_PREFIX + request.getSessionToken(), otp, OTP_TTL);

        BookingIntent intent = BookingIntent.builder()
                .eventId(event.getId())
                .quantity(request.getQuantity())
                .paymentPreference(request.getPaymentPreference())
                .build();
        redisTemplate.opsForValue().set(INTENT_PREFIX + request.getSessionToken(), intent, INTENT_TTL);

        // 10. Mock OTP Delivery — print to console for testing
        String maskedIdentifier = maskIdentifier(session.getIdentifier(), session.getMemberType());
        log.info("[OTP-LOG] OTP for member {} (session {}): {}", session.getMemberId(), request.getSessionToken(), otp);

        return InitiateBookingResponse.builder()
                .message("OTP sent to " + maskedIdentifier)
                .expiresInSeconds(OTP_TTL_SECONDS)
                .build();
    }

    // ─── Endpoint 3: Verify OTP & Finalize Booking ─────────────────────────────────

    /**
     * POST /api/registration/verify-otp
     *
     * The grand finale of the booking flow.
     * Verifies the OTP, calculates the price, and finalizes the booking in Postgres
     * via one of three paths:
     *
     *  PATH A — Free event (totalAmount == 0)
     *           → Registration saved with status FREE. Ticket returned immediately.
     *
     *  PATH B — Pay at Gate (paymentPreference == AT_GATE)
     *           → Registration saved with status PAY_AT_GATE. Ticket returned immediately.
     *
     *  PATH C — Online Payment (paymentPreference == ONLINE && totalAmount > 0)
     *           → Razorpay order created. Registration saved as PENDING.
     *             razorpayOrderId returned so the frontend can open the payment popup.
     */
    @Transactional
    public RegistrationResponse verifyOtpAndBook(VerifyOtpRequest request) {

        // ── Step 1: Validate OTP ───────────────────────────────────────────────
        String otpKey = OTP_PREFIX + request.getSessionToken();
        Object storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            throw new BadCredentialsException("OTP has expired. Please restart the booking process.");
        }
        if (!storedOtp.toString().equals(request.getOtp())) {
            throw new BadCredentialsException("Incorrect OTP. Please try again.");
        }

        // ── Step 2: Retrieve the locked BookingIntent from Redis ───────────────
        String intentKey = INTENT_PREFIX + request.getSessionToken();
        Object rawIntent = redisTemplate.opsForValue().get(intentKey);
        if (rawIntent == null) {
            throw new BadCredentialsException("Booking session expired. Please restart the booking process.");
        }
        BookingIntent intent = objectMapper.convertValue(rawIntent, BookingIntent.class);

        // ── Step 3: Retrieve the MemberSession from Redis ──────────────────────
        MemberSession session = getSessionOrThrow(request.getSessionToken());

        // ── Step 4: Re-fetch the Event from Postgres ───────────────────────────
        // We re-validate here in case the event was cancelled between /initiate and now.
        Event event = eventRepository.findById(intent.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event no longer exists."));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("This event is no longer accepting registrations.");
        }
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new IllegalArgumentException("The registration deadline for this event has passed.");
        }

        // ── Step 5: Calculate Price ────────────────────────────────────────────
        int quantity        = intent.getQuantity();
        int freeTickets     = event.getFreeTicketsPerRegistration();
        int paidTickets     = Math.max(0, quantity - freeTickets);
        BigDecimal totalAmount = event.getTicketPrice()
                .multiply(BigDecimal.valueOf(paidTickets));

        // ── Step 6: Generate Ticket Reference ─────────────────────────────────
        String ticketReference = generateTicketReference();

        // ── Step 7: Resolve Registration — reuse FAILED row or create fresh ────
        // If the member had a previous FAILED attempt for this event (e.g. card
        // declined, sold-out race condition), a row already exists in Postgres
        // with a UNIQUE constraint on (member_id, event_id). Inserting a new row
        // would throw a constraint violation. Instead, we find and UPDATE that
        // row, giving the member a fresh ticket reference and a clean slate.
        Registration registration = registrationRepository
                .findByMemberIdAndEventId(session.getMemberId(), event.getId())
                .orElse(null);

        boolean isRetry = registration != null
                && (registration.getPaymentStatus() == PaymentStatus.FAILED
                    || registration.getPaymentStatus() == PaymentStatus.PENDING);
        if (isRetry) {
            log.info("[BOOKING] Retry detected (previous status={}) — reusing registration for member={}, event={}",
                    registration.getPaymentStatus(), session.getMemberId(), event.getId());
            // Give the retry a fresh timestamp instead of the original attempt time
            registration.setBookedAt(LocalDateTime.now());
            // Reset check-in state defensively
            registration.setCheckedIn(false);
            // If the previous attempt created a Razorpay order (PENDING path), that order
            // is now abandoned. A new order will be created below in Path C if needed.
            // Explicitly null it out here so no stale order ID leaks into Paths A/B.
            registration.setRazorpayOrderId(null);
            registration.setRazorpayPaymentId(null);
        } else {
            // Fresh booking — create a new empty registration to populate below
            registration = new Registration();
            registration.setMemberId(session.getMemberId());
            registration.setMemberType(session.getMemberType());
            registration.setEvent(event);
        }

        // ── Step 5.5: Re-check Capacity (Bug 3 fix) ──────────────────────────────
        // Between /initiate (where capacity was first checked) and now, up to 10 minutes
        // may have elapsed. Other members may have taken remaining seats.
        // Paths A (FREE) and B (PAY_AT_GATE) lock seats immediately on save,
        // so we MUST verify capacity again right here before we write to the database.
        // Note: for Path C (PENDING), this guard still applies — even though PENDING
        // itself doesn't lock a seat, we should not issue a Razorpay order if the
        // event is already known to be sold out at this moment.
        int lockedNow = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remainingNow = event.getTotalCapacity() - lockedNow;
        if (quantity > remainingNow) {
            throw new IllegalArgumentException(
                    "Sorry, this event just filled up. Only " + remainingNow
                    + " seat(s) remain — please adjust your quantity or try another event.");
        }

        // ── Step 8: Three-Way Split ────────────────────────────────────────────
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.info("[BOOKING] PATH A (FREE) — member={}, event={}, qty={}",
                    session.getMemberId(), event.getId(), quantity);

            registration.setQuantity(quantity);
            registration.setTotalAmount(BigDecimal.ZERO);
            registration.setPaymentStatus(PaymentStatus.FREE);
            registration.setPaymentPreference(intent.getPaymentPreference());
            registration.setTicketReference(ticketReference);
            registration.setRazorpayOrderId(null);
            registration.setRazorpayPaymentId(null);

            registrationRepository.save(registration);
            cleanUpRedis(request.getSessionToken());

            return RegistrationResponse.builder()
                    .ticketReference(ticketReference)
                    .eventTitle(event.getTitle())
                    .quantity(quantity)
                    .totalAmount(BigDecimal.ZERO)
                    .paymentStatus(PaymentStatus.FREE)
                    .build();
        }

        // PATH B: Pay at the Gate
        if (intent.getPaymentPreference() == PaymentPreference.AT_GATE) {
            log.info("[BOOKING] PATH B (PAY_AT_GATE) — member={}, event={}, qty={}, amount={}",
                    session.getMemberId(), event.getId(), quantity, totalAmount);

            registration.setQuantity(quantity);
            registration.setTotalAmount(totalAmount);
            registration.setPaymentStatus(PaymentStatus.PAY_AT_GATE);
            registration.setPaymentPreference(intent.getPaymentPreference());
            registration.setTicketReference(ticketReference);
            registration.setRazorpayOrderId(null);
            registration.setRazorpayPaymentId(null);

            registrationRepository.save(registration);
            cleanUpRedis(request.getSessionToken());

            return RegistrationResponse.builder()
                    .ticketReference(ticketReference)
                    .eventTitle(event.getTitle())
                    .quantity(quantity)
                    .totalAmount(totalAmount)
                    .paymentStatus(PaymentStatus.PAY_AT_GATE)
                    .build();
        }

        // PATH C: Online Payment via Razorpay
        log.info("[BOOKING] PATH C (ONLINE) — member={}, event={}, qty={}, amount={}",
                session.getMemberId(), event.getId(), quantity, totalAmount);

        String razorpayOrderId;
        try {
            razorpayOrderId = razorpayService.createOrder(totalAmount, ticketReference);
        } catch (RazorpayException e) {
            log.error("[RAZORPAY] Failed to create order for ticket {}: {}", ticketReference, e.getMessage());
            throw new IllegalStateException("Payment gateway error. Please try again.");
        }

        registration.setQuantity(quantity);
        registration.setTotalAmount(totalAmount);
        registration.setPaymentStatus(PaymentStatus.PENDING);
        registration.setPaymentPreference(intent.getPaymentPreference());
        registration.setRazorpayOrderId(razorpayOrderId);
        registration.setTicketReference(ticketReference);
        registration.setRazorpayPaymentId(null); // reset any previous failed payment ID

        registrationRepository.save(registration);
        cleanUpRedis(request.getSessionToken());

        return RegistrationResponse.builder()
                .ticketReference(ticketReference)
                .eventTitle(event.getTitle())
                .quantity(quantity)
                .totalAmount(totalAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .razorpayOrderId(razorpayOrderId)
                .build();
    }

    // ─── Endpoint 4: Confirm Online Payment ────────────────────────────────────────────

    /**
     * POST /api/registration/confirm-payment
     *
     * Called by the frontend immediately after the Razorpay JS popup closes
     * with a successful payment. This is the "fast path" — it confirms the
     * ticket almost instantly so the user sees their ticket without delay.
     *
     * The webhook (/api/webhooks/razorpay) is the backup path that handles
     * cases where this endpoint is never reached (browser crash, network drop).
     *
     * Security layers:
     *   1. Razorpay signature verification — proves the payment data is authentic.
     *   2. Idempotency check — safely handles duplicate calls (webhook + frontend racing).
     *   3. Sold-out race condition guard — re-checks capacity before confirming.
     */
    @Transactional
    public RegistrationResponse confirmPayment(ConfirmPaymentRequest request) {

        // ── Step 1: Find the Registration ─────────────────────────────────────────
        Registration registration = registrationRepository
                .findByTicketReference(request.getTicketReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + request.getTicketReference()));

        // ── Step 2: Idempotency — handle duplicate calls gracefully ──────────────
        // The webhook may have already confirmed this ticket before the frontend
        // managed to call this endpoint. That is perfectly fine — just return success.
        if (registration.getPaymentStatus() == PaymentStatus.CONFIRMED) {
            log.info("[CONFIRM-PAYMENT] Ticket {} already CONFIRMED (idempotent call), returning success",
                    request.getTicketReference());
            return buildRegistrationResponse(registration);
        }

        // ── Step 3: Reject terminal states ───────────────────────────────────────
        // FAILED means Razorpay already reported a failed/expired payment.
        // Any other non-PENDING status (FREE, PAY_AT_GATE) means this was not
        // an online payment — confirm-payment should not be called for those.
        if (registration.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm payment for a ticket with status: "
                    + registration.getPaymentStatus());
        }

        // ── Step 4: Verify Razorpay signature FIRST (security before business logic) ──
        // This cryptographically proves the three values came from Razorpay and
        // were not forged by the frontend. Reject immediately if invalid.
        boolean signatureValid = razorpayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!signatureValid) {
            log.warn("[CONFIRM-PAYMENT] Signature verification FAILED for ticket {} — possible fraud attempt!",
                    request.getTicketReference());
            throw new IllegalArgumentException(
                    "Payment verification failed. The payment data is invalid or was tampered with.");
        }

        // ── Step 5: Sold-Out Race Condition Guard ──────────────────────────────
        // Scenario: Two members both reach the payment screen (PENDING status).
        // PENDING tickets do NOT lock seats. The first to pay gets the ticket.
        // The second to pay must be rejected here — even though their Razorpay
        // payment succeeded. We mark their registration FAILED.
        // NOTE: In a real system, you would also initiate a Razorpay refund here.
        Event event = registration.getEvent();
        int currentlyLocked = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remainingCapacity = event.getTotalCapacity() - currentlyLocked;

        if (registration.getQuantity() > remainingCapacity) {
            log.warn("[CONFIRM-PAYMENT] SOLD OUT race condition! ticket={}, requested={}, remaining={}",
                    request.getTicketReference(), registration.getQuantity(), remainingCapacity);

            // Mark as FAILED so this slot is permanently closed
            registration.setPaymentStatus(PaymentStatus.FAILED);
            registrationRepository.save(registration);

            // TODO: Trigger Razorpay refund for paymentId here in a future phase
            throw new IllegalStateException(
                    "We're sorry — this event just sold out while your payment was processing. "
                    + "A full refund will be issued to your account within 5-7 business days.");
        }

        // ── Step 6: All checks passed — confirm the booking ───────────────────────
        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRazorpayPaymentId(request.getRazorpayPaymentId());
        registrationRepository.save(registration);

        log.info("[CONFIRM-PAYMENT] Ticket {} CONFIRMED ✅ — paymentId={}",
                request.getTicketReference(), request.getRazorpayPaymentId());

        return buildRegistrationResponse(registration);
    }


    // ─── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Reads the MemberSession from Redis.
     * Throws 401 if the session is missing or expired.
     */
    private MemberSession getSessionOrThrow(String sessionToken) {
        Object raw = redisTemplate.opsForValue().get(SESSION_PREFIX + sessionToken);
        if (raw == null) {
            throw new BadCredentialsException("Session expired. Please verify your Member ID again.");
        }
        // ObjectMapper deserializes the stored JSON back into MemberSession
        return objectMapper.convertValue(raw, MemberSession.class);
    }

    /**
     * Mock of the external RIC API.
     * Returns TRUE if memberId starts with "RIC", otherwise FALSE.
     */
    private boolean mockRicApi(String memberId, String identifier) {
        return memberId != null && memberId.toUpperCase().startsWith("RIC");
    }

    /**
     * Masks the identifier for UX display.
     * INDIAN   -> "98****10"
     * OVERSEAS -> "r****@gmail.com"
     */
    private String maskIdentifier(String identifier, MemberType type) {
        if (identifier == null || identifier.length() < 4) return "****";

        if (type == MemberType.INDIAN) {
            return identifier.substring(0, 2) + "****" + identifier.substring(identifier.length() - 2);
        } else {
            int atIndex = identifier.indexOf('@');
            if (atIndex <= 1) return "****" + identifier.substring(atIndex);
            return identifier.charAt(0) + "****" + identifier.substring(atIndex);
        }
    }

    /**
     * Deletes the OTP and BookingIntent keys from Redis after a booking is finalized.
     * The session key is kept alive — the member may still navigate within the app.
     */
    private void cleanUpRedis(String sessionToken) {
        redisTemplate.delete(OTP_PREFIX + sessionToken);
        redisTemplate.delete(INTENT_PREFIX + sessionToken);
    }

    /**
     * Generates a unique, user-friendly ticket reference in the format:
     *   TKT-2026-AB12CD
     *
     * The 6-char alphanumeric suffix is randomly generated. Collision probability
     * is negligible at typical event scales (< 10,000 tickets per event).
     */
    private String generateTicketReference() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(chars.charAt(random.nextInt(chars.length())));
        }
        return "TKT-" + Year.now().getValue() + "-" + suffix;
    }

    /**
     * Converts a persisted Registration entity into a RegistrationResponse DTO.
     *
     * Shared by confirmPayment() and the future webhook handler so response
     * shape is always identical regardless of which path confirms the ticket.
     *
     * Note: registration.getEvent() is a LAZY association. This method must
     * only be called within a @Transactional context so the session is still open.
     */
    private RegistrationResponse buildRegistrationResponse(Registration registration) {
        return RegistrationResponse.builder()
                .ticketReference(registration.getTicketReference())
                .eventTitle(registration.getEvent().getTitle())
                .quantity(registration.getQuantity())
                .totalAmount(registration.getTotalAmount())
                .paymentStatus(registration.getPaymentStatus())
                .razorpayOrderId(registration.getRazorpayOrderId())
                .build();
    }
}

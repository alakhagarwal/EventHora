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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional
    public AdminBookingResponse registerMemberByAdmin(
            AdminBookingRequest request,
            String bookedByEmail) {

        if (!mockRicApi(request.getMemberId())) {
            throw new IllegalArgumentException(
                    "Invalid Member ID '" + request.getMemberId() +
                    "'. Must start with 'RIC' (e.g. RIC-2024-04512).");
        }

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

        if (request.getQuantity() > event.getMaxTicketsPerMember()) {
            throw new IllegalArgumentException(
                    "Maximum tickets per member for this event is " +
                    event.getMaxTicketsPerMember() + ". Requested: " + request.getQuantity() + ".");
        }

        int lockedTickets = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remaining     = event.getTotalCapacity() - lockedTickets;
        if (request.getQuantity() > remaining) {
            throw new IllegalArgumentException(
                    "Not enough seats available for '" + event.getTitle() +
                    "'. Only " + remaining + " seat(s) remain.");
        }

        boolean existingBooking = registrationRepository
                .findByMemberIdAndEventId(request.getMemberId(), event.getId())
                .isPresent();
        if (existingBooking) {
            throw new IllegalStateException(
                    "A registration already exists for member '" + request.getMemberId() +
                    "' in event '" + event.getTitle() +
                    "'. Check the registrations list before creating a new one.");
        }

        int quantity    = request.getQuantity();
        int freeTickets = event.getFreeTicketsPerRegistration();
        int paidTickets = Math.max(0, quantity - freeTickets);
        BigDecimal calculatedAmount = event.getTicketPrice()
                .multiply(BigDecimal.valueOf(paidTickets));

        final PaymentStatus paymentStatus;
        final BigDecimal    totalAmount;

        boolean isFreeEvent = calculatedAmount.compareTo(BigDecimal.ZERO) == 0;

        if (isFreeEvent) {

            paymentStatus = PaymentStatus.FREE;
            totalAmount   = BigDecimal.ZERO;
            log.info("[ADMIN-BOOKING] PATH FREE — bookedBy={}, member={}, event='{}', qty={}",
                    bookedByEmail, request.getMemberId(), event.getTitle(), quantity);

        } else if (request.getAction() == AdminBookingAction.COMPLIMENTARY) {

            paymentStatus = PaymentStatus.COMPLIMENTARY;
            totalAmount   = BigDecimal.ZERO;
            log.info("[ADMIN-BOOKING] PATH COMPLIMENTARY — bookedBy={}, member={}, event='{}', qty={}, waived={}",
                    bookedByEmail, request.getMemberId(), event.getTitle(), quantity, calculatedAmount);

        } else {

            paymentStatus = PaymentStatus.PAY_AT_GATE;
            totalAmount   = calculatedAmount;
            log.info("[ADMIN-BOOKING] PATH PAY_AT_GATE — bookedBy={}, member={}, event='{}', qty={}, amount={}",
                    bookedByEmail, request.getMemberId(), event.getTitle(), quantity, totalAmount);
        }

        String ticketReference = generateTicketReference();

        Registration registration = Registration.builder()
                .memberId(request.getMemberId())
                .memberType(request.getMemberType())
                .event(event)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .paymentStatus(paymentStatus)
                .paymentPreference(PaymentPreference.AT_GATE)
                .ticketReference(ticketReference)
                .razorpayOrderId(null)
                .razorpayPaymentId(null)
                .isCheckedIn(false)
                .build();

        registrationRepository.save(registration);

        log.info("[ADMIN-BOOKING] SAVED — ticket={}, member={}, event='{}', status={}, bookedBy={}",
                ticketReference, request.getMemberId(), event.getTitle(), paymentStatus, bookedByEmail);

        return AdminBookingResponse.builder()
                .ticketReference(ticketReference)
                .quantity(quantity)
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

    private boolean mockRicApi(String memberId) {
        return memberId != null && memberId.toUpperCase().startsWith("RIC");
    }

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

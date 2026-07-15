package com.eventHora.backend.service;

import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.Enum.MemberType;
import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.Enum.PaymentStatus;
import com.eventHora.backend.dto.BookingIntent;
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
        boolean alreadyBooked = registrationRepository
                .findByMemberIdAndEventId(session.getMemberId(), event.getId())
                .map(reg -> reg.getPaymentStatus() != PaymentStatus.FAILED)
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

        // ── Step 7: Three-Way Split ────────────────────────────────────────────
        Registration registration;

        // PATH A: Completely FREE
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.info("[BOOKING] PATH A (FREE) — member={}, event={}, qty={}",
                    session.getMemberId(), event.getId(), quantity);

            registration = Registration.builder()
                    .memberId(session.getMemberId())
                    .memberType(session.getMemberType())
                    .event(event)
                    .quantity(quantity)
                    .totalAmount(BigDecimal.ZERO)
                    .paymentStatus(PaymentStatus.FREE)
                    .paymentPreference(intent.getPaymentPreference())
                    .ticketReference(ticketReference)
                    .build();

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

            registration = Registration.builder()
                    .memberId(session.getMemberId())
                    .memberType(session.getMemberType())
                    .event(event)
                    .quantity(quantity)
                    .totalAmount(totalAmount)
                    .paymentStatus(PaymentStatus.PAY_AT_GATE)
                    .paymentPreference(intent.getPaymentPreference())
                    .ticketReference(ticketReference)
                    .build();

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

        registration = Registration.builder()
                .memberId(session.getMemberId())
                .memberType(session.getMemberType())
                .event(event)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentPreference(intent.getPaymentPreference())
                .razorpayOrderId(razorpayOrderId)
                .ticketReference(ticketReference)
                .build();

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
}

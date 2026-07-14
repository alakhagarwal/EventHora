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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
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

    // ─── Endpoint 3: Verify OTP & Confirm Booking ─────────────────────────────

    /**
     * POST /api/registration/verify-otp
     *
     * Validates the OTP, creates the Registration record, and returns ticket details.
     */
    public RegistrationResponse verifyOtp(VerifyOtpRequest request) {

        // 1. Validate session
        MemberSession session = getSessionOrThrow(request.getSessionToken());

        // 2. Get stored OTP from Redis
        Object storedOtpObj = redisTemplate.opsForValue().get(OTP_PREFIX + request.getSessionToken());
        if (storedOtpObj == null) {
            throw new IllegalArgumentException("OTP has expired. Please start a new booking.");
        }
        String storedOtp = storedOtpObj.toString();

        // 3. Compare OTP
        if (!storedOtp.equals(request.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP. Please try again.");
        }

        // 4. Get booking intent from Redis
        Object intentObj = redisTemplate.opsForValue().get(INTENT_PREFIX + request.getSessionToken());
        if (intentObj == null) {
            throw new IllegalArgumentException("Booking session expired. Please start a new booking.");
        }
        BookingIntent intent = objectMapper.convertValue(intentObj, BookingIntent.class);

        // 5. Fetch event
        Event event = eventRepository.findById(intent.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // 6. Calculate total amount
        int paidTickets = Math.max(0, intent.getQuantity() - event.getFreeTicketsPerRegistration());
        BigDecimal totalAmount = event.getTicketPrice().multiply(BigDecimal.valueOf(paidTickets));

        // 7. Determine payment status
        PaymentStatus paymentStatus;
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = PaymentStatus.FREE;
        } else if (intent.getPaymentPreference() == PaymentPreference.AT_GATE) {
            paymentStatus = PaymentStatus.PAY_AT_GATE;
        } else {
            paymentStatus = PaymentStatus.PENDING;
        }

        // 8. Generate ticket reference
        String ticketRef = "TKT-" + LocalDateTime.now().getYear() + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // 9. Create and save Registration
        Registration registration = Registration.builder()
                .memberId(session.getMemberId())
                .memberType(session.getMemberType())
                .event(event)
                .quantity(intent.getQuantity())
                .totalAmount(totalAmount)
                .paymentStatus(paymentStatus)
                .paymentPreference(intent.getPaymentPreference())
                .ticketReference(ticketRef)
                .build();
        registrationRepository.save(registration);

        // 10. Clean up Redis keys
        redisTemplate.delete(OTP_PREFIX + request.getSessionToken());
        redisTemplate.delete(INTENT_PREFIX + request.getSessionToken());

        log.info("[BOOKING] Member {} booked {} ticket(s) for '{}' — ref: {} — status: {}",
                session.getMemberId(), intent.getQuantity(), event.getTitle(), ticketRef, paymentStatus);

        return RegistrationResponse.builder()
                .ticketReference(ticketRef)
                .eventTitle(event.getTitle())
                .quantity(intent.getQuantity())
                .totalAmount(totalAmount)
                .paymentStatus(paymentStatus)
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
}

package com.eventHora.backend.service;

import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.Enum.MemberType;
import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.Enum.PaymentStatus;
import com.eventHora.backend.dto.BookingIntent;
import com.eventHora.backend.dto.CheckInRequest;
import com.eventHora.backend.dto.CheckInResponse;
import com.eventHora.backend.dto.ConfirmPaymentRequest;
import com.eventHora.backend.dto.InitiateBookingRequest;
import com.eventHora.backend.dto.InitiateBookingResponse;
import com.eventHora.backend.dto.MemberSession;
import com.eventHora.backend.dto.MyBookingResponse;
import com.eventHora.backend.dto.RecordPaymentRequest;
import com.eventHora.backend.dto.RegistrationResponse;
import com.eventHora.backend.dto.RegistrationSummaryResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
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

    private static final String SESSION_PREFIX = "session:";
    private static final String OTP_PREFIX     = "otp:";
    private static final String INTENT_PREFIX  = "intent:";

    private static final Duration SESSION_TTL = Duration.ofHours(1);
    private static final Duration OTP_TTL     = Duration.ofMinutes(5);
    private static final Duration INTENT_TTL  = Duration.ofMinutes(10);

    private static final int OTP_TTL_SECONDS = 300;

    @Value("${demo.mode:false}")
    private boolean demoMode;

    public VerifyMemberResponse verifyMember(VerifyMemberRequest request) {

        if (request.getMemberType() == MemberType.INDIAN) {
            if (!request.getIdentifier().matches("^(\\+91[\\-\\s]?)?[0-9]{10}$")) {
                throw new IllegalArgumentException("Invalid mobile number format. Must be a valid Indian mobile number.");
            }
        } else if (request.getMemberType() == MemberType.OVERSEAS) {
            if (!request.getIdentifier().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("Invalid email format.");
            }
        }

        boolean isValid = mockRicApi(request.getMemberId(), request.getIdentifier());
        if (!isValid) {
            throw new IllegalArgumentException("Invalid Member ID or Identifier");
        }

        String sessionToken = UUID.randomUUID().toString();
        MemberSession session = MemberSession.builder()
                .sessionToken(sessionToken)
                .memberId(request.getMemberId())
                .identifier(request.getIdentifier())
                .memberType(request.getMemberType())
                .build();

        redisTemplate.opsForValue().set(SESSION_PREFIX + sessionToken, session, SESSION_TTL);

        return VerifyMemberResponse.builder()
                .sessionToken(sessionToken)
                .memberId(request.getMemberId())
                .memberType(request.getMemberType())
                .maskedIdentifier(maskIdentifier(request.getIdentifier(), request.getMemberType()))
                .build();
    }

    public InitiateBookingResponse initiateBooking(InitiateBookingRequest request) {

        MemberSession session = getSessionOrThrow(request.getSessionToken());

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("This event is no longer accepting registrations.");
        }

        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new IllegalArgumentException("The registration deadline for this event has passed.");
        }

        if (request.getQuantity() > event.getMaxTicketsPerMember()) {
            throw new IllegalArgumentException(
                "You can book a maximum of " + event.getMaxTicketsPerMember() + " tickets for this event."
            );
        }

        int lockedTickets = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remaining = event.getTotalCapacity() - lockedTickets;
        if (request.getQuantity() > remaining) {
            throw new IllegalArgumentException(
                "Not enough seats available. Only " + remaining + " seat(s) remain."
            );
        }

        boolean alreadyBooked = registrationRepository
                .findByMemberIdAndEventId(session.getMemberId(), event.getId())
                .map(reg -> reg.getPaymentStatus() != PaymentStatus.FAILED
                         && reg.getPaymentStatus() != PaymentStatus.PENDING)
                .orElse(false);
        if (alreadyBooked) {
            throw new IllegalStateException("You have already registered for this event.");
        }

        String otp = String.format("%06d", new Random().nextInt(999999));

        redisTemplate.opsForValue().set(OTP_PREFIX + request.getSessionToken(), otp, OTP_TTL);

        BookingIntent intent = BookingIntent.builder()
                .eventId(event.getId())
                .quantity(request.getQuantity())
                .paymentPreference(request.getPaymentPreference())
                .build();
        redisTemplate.opsForValue().set(INTENT_PREFIX + request.getSessionToken(), intent, INTENT_TTL);

        String maskedIdentifier = maskIdentifier(session.getIdentifier(), session.getMemberType());
        log.info("[OTP-LOG] OTP for member {} (session {}): {}", session.getMemberId(), request.getSessionToken(), otp);

        return InitiateBookingResponse.builder()
                .message("OTP sent to " + maskedIdentifier)
                .expiresInSeconds(OTP_TTL_SECONDS)
                .build();
    }

    @Transactional
    public RegistrationResponse verifyOtpAndBook(VerifyOtpRequest request) {

        String otpKey = OTP_PREFIX + request.getSessionToken();
        Object storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {

            if (demoMode && "123456".equals(request.getOtp())) {
                log.info("[DEMO-MODE] Universal demo OTP accepted for session {}", request.getSessionToken());
            } else {
                throw new BadCredentialsException("OTP has expired. Please restart the booking process.");
            }
        } else {

            boolean isDemoOtp = demoMode && "123456".equals(request.getOtp());
            if (!isDemoOtp && !storedOtp.toString().equals(request.getOtp())) {
                throw new BadCredentialsException("Incorrect OTP. Please try again.");
            }
            if (isDemoOtp) {
                log.info("[DEMO-MODE] Universal demo OTP accepted for session {}", request.getSessionToken());
            }
        }

        String intentKey = INTENT_PREFIX + request.getSessionToken();
        Object rawIntent = redisTemplate.opsForValue().get(intentKey);
        if (rawIntent == null) {
            throw new BadCredentialsException("Booking session expired. Please restart the booking process.");
        }
        BookingIntent intent = objectMapper.convertValue(rawIntent, BookingIntent.class);

        MemberSession session = getSessionOrThrow(request.getSessionToken());

        Event event = eventRepository.findById(intent.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event no longer exists."));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("This event is no longer accepting registrations.");
        }
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new IllegalArgumentException("The registration deadline for this event has passed.");
        }

        int quantity        = intent.getQuantity();
        int freeTickets     = event.getFreeTicketsPerRegistration();
        int paidTickets     = Math.max(0, quantity - freeTickets);
        BigDecimal totalAmount = event.getTicketPrice()
                .multiply(BigDecimal.valueOf(paidTickets));

        String ticketReference = generateTicketReference();

        Registration registration = registrationRepository
                .findByMemberIdAndEventId(session.getMemberId(), event.getId())
                .orElse(null);

        boolean isRetry = registration != null
                && (registration.getPaymentStatus() == PaymentStatus.FAILED
                    || registration.getPaymentStatus() == PaymentStatus.PENDING);
        if (isRetry) {
            log.info("[BOOKING] Retry detected (previous status={}) — reusing registration for member={}, event={}",
                    registration.getPaymentStatus(), session.getMemberId(), event.getId());

            registration.setBookedAt(LocalDateTime.now());

            registration.setCheckedIn(false);

            registration.setRazorpayOrderId(null);
            registration.setRazorpayPaymentId(null);
        } else {

            registration = new Registration();
            registration.setMemberId(session.getMemberId());
            registration.setMemberType(session.getMemberType());
            registration.setEvent(event);
        }

        int lockedNow = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remainingNow = event.getTotalCapacity() - lockedNow;
        if (quantity > remainingNow) {
            throw new IllegalArgumentException(
                    "Sorry, this event just filled up. Only " + remainingNow
                    + " seat(s) remain — please adjust your quantity or try another event.");
        }

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
        registration.setRazorpayPaymentId(null);

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

    @Transactional
    public RegistrationResponse confirmPayment(ConfirmPaymentRequest request) {

        Registration registration = registrationRepository
                .findByTicketReference(request.getTicketReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + request.getTicketReference()));

        if (registration.getPaymentStatus() == PaymentStatus.CONFIRMED) {
            log.info("[CONFIRM-PAYMENT] Ticket {} already CONFIRMED (idempotent call), returning success",
                    request.getTicketReference());
            return buildRegistrationResponse(registration);
        }

        if (registration.getPaymentStatus() == PaymentStatus.FREE
                || registration.getPaymentStatus() == PaymentStatus.PAY_AT_GATE) {
            throw new IllegalStateException(
                    "Cannot confirm payment for a ticket with status: "
                    + registration.getPaymentStatus());
        }

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

        Event event = registration.getEvent();
        int currentlyLocked = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remainingCapacity = event.getTotalCapacity() - currentlyLocked;

        if (registration.getQuantity() > remainingCapacity) {
            log.warn("[CONFIRM-PAYMENT] SOLD OUT race condition! ticket={}, requested={}, remaining={}",
                    request.getTicketReference(), registration.getQuantity(), remainingCapacity);

            registration.setPaymentStatus(PaymentStatus.FAILED);
            registrationRepository.save(registration);

            try {
                razorpayService.initiateRefund(request.getRazorpayPaymentId());
            } catch (RazorpayException e) {
                log.error("[CONFIRM-PAYMENT] ⚠️  REFUND FAILED — MANUAL ACTION REQUIRED! " +
                          "paymentId={}, ticket={}, error={}",
                          request.getRazorpayPaymentId(),
                          request.getTicketReference(),
                          e.getMessage());
            }
            throw new IllegalStateException(
                    "We're sorry — this event just sold out while your payment was processing. "
                    + "A full refund will be issued to your account within 5-7 business days.");
        }

        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRazorpayPaymentId(request.getRazorpayPaymentId());
        registrationRepository.save(registration);

        log.info("[CONFIRM-PAYMENT] Ticket {} CONFIRMED ✅ — paymentId={}",
                request.getTicketReference(), request.getRazorpayPaymentId());

        return buildRegistrationResponse(registration);
    }

    @Transactional
    public void handleRazorpayWebhook(String rawBody) {
        try {

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(rawBody);
            String eventType = root.path("event").asText();

            log.info("[WEBHOOK] Received Razorpay event: {}", eventType);

            switch (eventType) {
                case "payment.captured" -> handlePaymentCaptured(root);
                case "payment.failed"   -> handlePaymentFailed(root);
                default -> log.info("[WEBHOOK] Ignoring unhandled event type: {}", eventType);
            }

        } catch (Exception e) {

            log.error("[WEBHOOK] Failed to process webhook payload: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentCaptured(com.fasterxml.jackson.databind.JsonNode root) {

        com.fasterxml.jackson.databind.JsonNode paymentEntity =
                root.path("payload").path("payment").path("entity");

        String razorpayPaymentId = paymentEntity.path("id").asText();
        String razorpayOrderId   = paymentEntity.path("order_id").asText();

        log.info("[WEBHOOK] payment.captured — orderId={}, paymentId={}", razorpayOrderId, razorpayPaymentId);

        Registration registration = registrationRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);

        if (registration == null) {
            log.warn("[WEBHOOK] No registration found for orderId={} — possibly already cleaned up or invalid.", razorpayOrderId);
            return;
        }

        if (registration.getPaymentStatus() == PaymentStatus.CONFIRMED) {
            log.info("[WEBHOOK] Ticket {} already CONFIRMED — skipping (idempotent).", registration.getTicketReference());
            return;
        }

        Event event = registration.getEvent();
        int locked    = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remaining = event.getTotalCapacity() - locked;

        if (registration.getQuantity() > remaining) {
            log.warn("[WEBHOOK] SOLD OUT race — ticket={}, requested={}, remaining={}",
                    registration.getTicketReference(), registration.getQuantity(), remaining);

            registration.setPaymentStatus(PaymentStatus.FAILED);
            registrationRepository.save(registration);

            try {
                razorpayService.initiateRefund(razorpayPaymentId);
            } catch (com.razorpay.RazorpayException e) {
                log.error("[WEBHOOK] ⚠️  REFUND FAILED — MANUAL ACTION REQUIRED! " +
                          "paymentId={}, ticket={}, error={}",
                          razorpayPaymentId, registration.getTicketReference(), e.getMessage());
            }
            return;
        }

        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRazorpayPaymentId(razorpayPaymentId);
        registrationRepository.save(registration);

        log.info("[WEBHOOK] Ticket {} CONFIRMED ✅ via webhook — paymentId={}",
                registration.getTicketReference(), razorpayPaymentId);
    }

    private void handlePaymentFailed(com.fasterxml.jackson.databind.JsonNode root) {
        com.fasterxml.jackson.databind.JsonNode paymentEntity =
                root.path("payload").path("payment").path("entity");

        String razorpayOrderId = paymentEntity.path("order_id").asText();
        String errorDescription = paymentEntity.path("error_description").asText("unknown reason");

        log.info("[WEBHOOK] payment.failed — orderId={}, reason={}", razorpayOrderId, errorDescription);

        Registration registration = registrationRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);

        if (registration == null) {
            log.warn("[WEBHOOK] No registration found for orderId={}", razorpayOrderId);
            return;
        }

        if (registration.getPaymentStatus() != PaymentStatus.PENDING) {
            log.info("[WEBHOOK] Ticket {} is in status {} — skipping payment.failed (idempotent or already terminal).",
                    registration.getTicketReference(), registration.getPaymentStatus());
            return;
        }

        registration.setPaymentStatus(PaymentStatus.FAILED);
        registrationRepository.save(registration);

        log.info("[WEBHOOK] Ticket {} marked FAILED via webhook — member can retry.", registration.getTicketReference());
    }

    private MemberSession getSessionOrThrow(String sessionToken) {
        Object raw = redisTemplate.opsForValue().get(SESSION_PREFIX + sessionToken);
        if (raw == null) {
            throw new BadCredentialsException("Session expired. Please verify your Member ID again.");
        }

        return objectMapper.convertValue(raw, MemberSession.class);
    }

    private boolean mockRicApi(String memberId, String identifier) {
        return memberId != null && memberId.toUpperCase().startsWith("RIC");
    }

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

    private void cleanUpRedis(String sessionToken) {
        redisTemplate.delete(OTP_PREFIX + sessionToken);
        redisTemplate.delete(INTENT_PREFIX + sessionToken);
    }

    private String generateTicketReference() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(chars.charAt(random.nextInt(chars.length())));
        }
        return "TKT-" + Year.now().getValue() + "-" + suffix;
    }

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

    @Transactional
    public CheckInResponse checkIn(CheckInRequest request) {

        Registration registration = registrationRepository
                .findByTicketReference(request.getTicketReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + request.getTicketReference()));

        PaymentStatus status = registration.getPaymentStatus();

        switch (status) {
            case PENDING -> throw new IllegalStateException(
                    "This ticket has an incomplete online payment. " +
                    "Please ask the member to make a new Pay-at-Gate booking if seats are still available.");
            case FAILED -> throw new IllegalStateException(
                    "This ticket's payment failed. The member does not have a valid booking.");
            case PAY_AT_GATE -> throw new IllegalStateException(
                    "Payment collection required before entry. " +
                    "Please collect the cash/card payment and " +
                    "confirm payment to check in the member.");
            default -> {

            }
        }

        if (registration.isCheckedIn()) {
            log.warn("Duplicate scan: ticket {} was already checked in at {}",
                    registration.getTicketReference(), registration.getCheckedInAt());
            return CheckInResponse.builder()
                    .ticketReference(registration.getTicketReference())
                    .memberId(registration.getMemberId())
                    .eventTitle(registration.getEvent().getTitle())
                    .quantity(registration.getQuantity())
                    .totalAmount(registration.getTotalAmount())
                    .paymentStatus(registration.getPaymentStatus())
                    .alreadyCheckedIn(true)
                    .checkedInAt(registration.getCheckedInAt())
                    .message("⚠️ Already checked in at " + registration.getCheckedInAt())
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        registration.setCheckedIn(true);
        registration.setCheckedInAt(now);
        registrationRepository.save(registration);

        log.info("Check-in: member {} admitted for event '{}' (ticket: {}, qty: {})",
                registration.getMemberId(),
                registration.getEvent().getTitle(),
                registration.getTicketReference(),
                registration.getQuantity());

        return CheckInResponse.builder()
                .ticketReference(registration.getTicketReference())
                .memberId(registration.getMemberId())
                .eventTitle(registration.getEvent().getTitle())
                .quantity(registration.getQuantity())
                .totalAmount(registration.getTotalAmount())
                .paymentStatus(registration.getPaymentStatus())
                .alreadyCheckedIn(false)
                .checkedInAt(now)
                .message("✅ Check-in successful")
                .build();
    }

    @Transactional
    public CheckInResponse recordGatePayment(RecordPaymentRequest request) {

        Registration registration = registrationRepository
                .findByTicketReference(request.getTicketReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + request.getTicketReference()));

        PaymentStatus currentStatus = registration.getPaymentStatus();

        if (currentStatus != PaymentStatus.PAY_AT_GATE) {
            String reason = switch (currentStatus) {
                case CONFIRMED     -> "This ticket has already been paid online and checked in via QR scan.";
                case FREE          -> "This is a free ticket — no payment collection needed. Use QR check-in.";
                case COMPLIMENTARY -> "This ticket has already been marked complimentary.";
                case PENDING       -> "This ticket has an incomplete online payment, not a Pay-at-Gate booking.";
                case FAILED        -> "This ticket's payment failed. The member does not have a valid booking.";
                default            -> "This ticket cannot be processed here (status: " + currentStatus + ").";
            };
            throw new IllegalStateException(reason);
        }

        PaymentStatus newStatus = switch (request.getAction()) {
            case "PAID"          -> PaymentStatus.CONFIRMED;
            case "COMPLIMENTARY" -> PaymentStatus.COMPLIMENTARY;
            default -> throw new IllegalArgumentException(
                    "Invalid action '" + request.getAction() + "'. Must be 'PAID' or 'COMPLIMENTARY'.");
        };

        LocalDateTime now = LocalDateTime.now();
        registration.setPaymentStatus(newStatus);
        registration.setCheckedIn(true);
        registration.setCheckedInAt(now);
        registrationRepository.save(registration);

        log.info("Gate payment: member {} — {} for event '{}' (ticket: {}, qty: {}, amount: {})",
                registration.getMemberId(),
                request.getAction(),
                registration.getEvent().getTitle(),
                registration.getTicketReference(),
                registration.getQuantity(),
                registration.getTotalAmount());

        String message = newStatus == PaymentStatus.CONFIRMED
                ? "✅ Payment recorded and member checked in"
                : "✅ Marked complimentary and member checked in";

        return CheckInResponse.builder()
                .ticketReference(registration.getTicketReference())
                .memberId(registration.getMemberId())
                .eventTitle(registration.getEvent().getTitle())
                .quantity(registration.getQuantity())
                .totalAmount(registration.getTotalAmount())
                .paymentStatus(newStatus)
                .alreadyCheckedIn(false)
                .checkedInAt(now)
                .message(message)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MyBookingResponse> getMyBookings(String sessionToken) {

        MemberSession session = getSessionOrThrow(sessionToken);

        List<Registration> registrations = registrationRepository
                .findByMemberIdOrderByBookedAtDesc(session.getMemberId());

        log.info("My Bookings: member {} has {} registration(s)", session.getMemberId(), registrations.size());

        return registrations.stream()
                .map(this::toMyBookingResponse)
                .toList();
    }

    private MyBookingResponse toMyBookingResponse(Registration r) {
        return MyBookingResponse.builder()
                .ticketReference(r.getTicketReference())
                .quantity(r.getQuantity())
                .totalAmount(r.getTotalAmount())
                .paymentStatus(r.getPaymentStatus())
                .paymentPreference(r.getPaymentPreference())
                .isCheckedIn(r.isCheckedIn())
                .checkedInAt(r.getCheckedInAt())

                .eventTitle(r.getEvent().getTitle())
                .eventDate(r.getEvent().getEventDate())
                .eventStartTime(r.getEvent().getStartTime())
                .eventVenue(r.getEvent().getVenue())
                .eventUniqueLink(r.getEvent().getUniqueEventLink())
                .bookedAt(r.getBookedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public RegistrationSummaryResponse lookupByTicketReference(String ticketReference) {

        Registration registration = registrationRepository
                .findByTicketReference(ticketReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + ticketReference));

        log.info("[LOOKUP] Ticket {} looked up by staff (memberId={}, status={})",
                ticketReference,
                registration.getMemberId(),
                registration.getPaymentStatus());

        return toRegistrationSummaryResponse(registration);
    }

    private RegistrationSummaryResponse toRegistrationSummaryResponse(Registration r) {
        return RegistrationSummaryResponse.builder()
                .registrationId(r.getId())
                .ticketReference(r.getTicketReference())
                .memberId(r.getMemberId())
                .memberType(r.getMemberType())
                .quantity(r.getQuantity())
                .totalAmount(r.getTotalAmount())
                .paymentStatus(r.getPaymentStatus())
                .paymentPreference(r.getPaymentPreference())
                .isCheckedIn(r.isCheckedIn())
                .checkedInAt(r.getCheckedInAt())
                .bookedAt(r.getBookedAt())
                .build();
    }

}

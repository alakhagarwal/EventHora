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

        // ── Step 3: Reject truly terminal states ─────────────────────────────────
        // CONFIRMED is handled above (idempotency).
        // FREE / PAY_AT_GATE were never online payments — /confirm-payment is irrelevant for them.
        //
        // FAILED is intentionally allowed through here.
        // Razorpay allows the user to retry a failed payment attempt on the SAME Razorpay order
        // (e.g. first try UPI → fails → try credit card → succeeds, all same order_id).
        // In that window, a payment.failed webhook may have already flipped our status to FAILED.
        // We must NOT block the subsequent success. The signature check in Step 4 below
        // cryptographically proves this is a genuine payment from Razorpay.
        if (registration.getPaymentStatus() == PaymentStatus.FREE
                || registration.getPaymentStatus() == PaymentStatus.PAY_AT_GATE) {
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

            // Initiate automatic full refund — best-effort.
            // If this call fails (network blip, Razorpay downtime), we log it loudly
            // for manual ops follow-up. We do NOT let a refund failure crash the response
            // because the registration is already saved as FAILED in the database.
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

        // ── Step 6: All checks passed — confirm the booking ───────────────────────
        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRazorpayPaymentId(request.getRazorpayPaymentId());
        registrationRepository.save(registration);

        log.info("[CONFIRM-PAYMENT] Ticket {} CONFIRMED ✅ — paymentId={}",
                request.getTicketReference(), request.getRazorpayPaymentId());

        return buildRegistrationResponse(registration);
    }


    // ─── Webhook Handler ──────────────────────────────────────────────────────

    /**
     * Handles an incoming verified Razorpay webhook event.
     *
     * WHY THIS EXISTS — The "Safety Net":
     *   The happy path is: user pays → Razorpay popup → frontend calls /confirm-payment.
     *   But what if the user's browser crashes, they lose internet, or they close the
     *   tab the instant payment succeeds? /confirm-payment is never called.
     *   Razorpay's server-to-server webhook fires regardless of the frontend. This
     *   method catches those "orphaned" payments and confirms the ticket automatically.
     *
     * IDEMPOTENCY:
     *   The webhook may be retried by Razorpay if we don't respond with 200 quickly.
     *   This method checks if the registration is already CONFIRMED before doing
     *   anything, so double-processing the same event is completely harmless.
     *
     * EVENTS WE HANDLE:
     *   - "payment.captured" → mark PENDING registration as CONFIRMED
     *   - "payment.failed"   → mark PENDING registration as FAILED
     *   - anything else      → log and ignore (200 OK is still returned to Razorpay
     *                          so it stops retrying events we don't care about)
     *
     * @param rawBody   The raw JSON webhook payload (already signature-verified by controller)
     */
    @Transactional
    public void handleRazorpayWebhook(String rawBody) {
        try {
            // Parse the raw payload into a JSON tree
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(rawBody);
            String eventType = root.path("event").asText();

            log.info("[WEBHOOK] Received Razorpay event: {}", eventType);

            switch (eventType) {
                case "payment.captured" -> handlePaymentCaptured(root);
                case "payment.failed"   -> handlePaymentFailed(root);
                default -> log.info("[WEBHOOK] Ignoring unhandled event type: {}", eventType);
            }

        } catch (Exception e) {
            // We log the error but do NOT rethrow — the controller must still
            // return 200 OK to Razorpay. If we return 4xx/5xx, Razorpay retries
            // for 24 hours, which can cause duplicate processing.
            log.error("[WEBHOOK] Failed to process webhook payload: {}", e.getMessage(), e);
        }
    }

    /**
     * payment.captured — The user's payment was captured successfully by Razorpay.
     * We confirm their ticket, applying the same sold-out guard as /confirm-payment.
     */
    private void handlePaymentCaptured(com.fasterxml.jackson.databind.JsonNode root) {
        // Navigate the Razorpay webhook payload structure:
        // root → payload → payment → entity → { id, order_id, ... }
        com.fasterxml.jackson.databind.JsonNode paymentEntity =
                root.path("payload").path("payment").path("entity");

        String razorpayPaymentId = paymentEntity.path("id").asText();
        String razorpayOrderId   = paymentEntity.path("order_id").asText();

        log.info("[WEBHOOK] payment.captured — orderId={}, paymentId={}", razorpayOrderId, razorpayPaymentId);

        // Find the registration by Razorpay order ID
        Registration registration = registrationRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);

        if (registration == null) {
            log.warn("[WEBHOOK] No registration found for orderId={} — possibly already cleaned up or invalid.", razorpayOrderId);
            return;
        }

        // Idempotency: if already confirmed (e.g. frontend already called /confirm-payment), do nothing
        if (registration.getPaymentStatus() == PaymentStatus.CONFIRMED) {
            log.info("[WEBHOOK] Ticket {} already CONFIRMED — skipping (idempotent).", registration.getTicketReference());
            return;
        }

        // Capacity guard — same as in confirmPayment()
        // PENDING tickets don't lock seats, so the event may have sold out
        // while this payment was in-flight.
        Event event = registration.getEvent();
        int locked    = registrationRepository.sumLockedTicketsForEvent(event.getId());
        int remaining = event.getTotalCapacity() - locked;

        if (registration.getQuantity() > remaining) {
            log.warn("[WEBHOOK] SOLD OUT race — ticket={}, requested={}, remaining={}",
                    registration.getTicketReference(), registration.getQuantity(), remaining);

            registration.setPaymentStatus(PaymentStatus.FAILED);
            registrationRepository.save(registration);

            // Initiate automatic refund — best-effort
            try {
                razorpayService.initiateRefund(razorpayPaymentId);
            } catch (com.razorpay.RazorpayException e) {
                log.error("[WEBHOOK] ⚠️  REFUND FAILED — MANUAL ACTION REQUIRED! " +
                          "paymentId={}, ticket={}, error={}",
                          razorpayPaymentId, registration.getTicketReference(), e.getMessage());
            }
            return;
        }

        // All checks passed — confirm the booking
        registration.setPaymentStatus(PaymentStatus.CONFIRMED);
        registration.setRazorpayPaymentId(razorpayPaymentId);
        registrationRepository.save(registration);

        log.info("[WEBHOOK] Ticket {} CONFIRMED ✅ via webhook — paymentId={}",
                registration.getTicketReference(), razorpayPaymentId);
    }

    /**
     * payment.failed — The user's payment failed on Razorpay's side (card declined,
     * net banking timeout, UPI failure, etc.). Mark the registration as FAILED so
     * the member can immediately retry without being blocked.
     */
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

        // Guard: only act on PENDING status.
        // FAILED   → idempotent (already failed, skip)
        // CONFIRMED → do NOT downgrade a confirmed ticket. A delayed or duplicate
        //             payment.failed webhook for the same order_id must never overwrite
        //             a ticket that was successfully confirmed by a subsequent payment
        //             attempt or by /confirm-payment.
        // FREE / PAY_AT_GATE → should never receive this webhook, but guard anyway.
        if (registration.getPaymentStatus() != PaymentStatus.PENDING) {
            log.info("[WEBHOOK] Ticket {} is in status {} — skipping payment.failed (idempotent or already terminal).",
                    registration.getTicketReference(), registration.getPaymentStatus());
            return;
        }

        registration.setPaymentStatus(PaymentStatus.FAILED);
        registrationRepository.save(registration);

        log.info("[WEBHOOK] Ticket {} marked FAILED via webhook — member can retry.", registration.getTicketReference());
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
// ─── Endpoint: Staff QR Check-In ─────────────────────────────────────────

    /**
     * POST /api/staff/checkin
     *
     * Called when a STAFF member scans a member's QR code at the event gate.
     *
     * Business rules:
     *  - Only tickets with a "locked" payment status (CONFIRMED, FREE,
     *    PAY_AT_GATE, COMPLIMENTARY) can be admitted.
     *  - PENDING → rejected with a specific message telling staff to redirect
     *    the member to make a new Pay-at-Gate booking.
     *  - FAILED  → rejected — the member's online payment did not go through.
     *  - If the ticket has already been checked in (duplicate scan) the method
     *    returns 200 with alreadyCheckedIn=true so the UI can show a warning
     *    without treating it as a hard error (accidental double-scan is common).
     */
    @Transactional
    public CheckInResponse checkIn(CheckInRequest request) {

        // 1. Look up the registration by ticketReference
        Registration registration = registrationRepository
                .findByTicketReference(request.getTicketReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + request.getTicketReference()));

        PaymentStatus status = registration.getPaymentStatus();

        // 2. Validate payment status
        //    PAY_AT_GATE is intentionally rejected here — staff must collect cash first
        //    via POST /api/staff/record-payment, which records payment and checks in atomically.
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
                // CONFIRMED, FREE, COMPLIMENTARY — valid for direct entry
            }
        }

        // 3. Idempotent duplicate-scan handling
        //    If already checked in, return the existing data with a warning flag.
        //    Do NOT flip the timestamp again — preserve the original check-in time.
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

        // 4. First-time check-in — record the timestamp and flip the flag
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

    // ─── Endpoint: Staff Record Gate Payment ──────────────────────────────────

    /**
     * POST /api/staff/record-payment
     *
     * Records cash (or complimentary) collection for a PAY_AT_GATE ticket
     * and simultaneously checks the member in.
     *
     * These two operations are intentionally atomic:
     *   - Collecting payment IS the act of admitting the member at the gate.
     *   - Staff does not need a second QR scan after recording payment.
     *
     * Business rules:
     *  - Only PAY_AT_GATE tickets can be processed here.
     *  - PAID action   → paymentStatus CONFIRMED   (cash/card collected)
     *  - COMP action   → paymentStatus COMPLIMENTARY (fee waived by staff)
     *  - Any other current status → rejected with an explanation.
     */
    @Transactional
    public CheckInResponse recordGatePayment(RecordPaymentRequest request) {

        // 1. Look up the registration
        Registration registration = registrationRepository
                .findByTicketReference(request.getTicketReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found: " + request.getTicketReference()));

        PaymentStatus currentStatus = registration.getPaymentStatus();

        // 2. Only PAY_AT_GATE tickets can be processed at this endpoint
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

        // 3. Apply the action
        PaymentStatus newStatus = switch (request.getAction()) {
            case "PAID"          -> PaymentStatus.CONFIRMED;
            case "COMPLIMENTARY" -> PaymentStatus.COMPLIMENTARY;
            default -> throw new IllegalArgumentException(
                    "Invalid action '" + request.getAction() + "'. Must be 'PAID' or 'COMPLIMENTARY'.");
        };

        // 4. Record payment + check in atomically
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
    // ─── Phase 8A: Member Self-Service — My Bookings ──────────────────────────

    /**
     * GET /api/registration/my-bookings?sessionToken={token}
     *
     * Returns a member's complete booking history across all events, newest first.
     *
     * The session token must be valid (in Redis). We do NOT accept the memberId
     * from the query param directly — the session is the source of truth to prevent
     * a member from peeking at another member's bookings by guessing their ID.
     *
     * All statuses are returned (PENDING, FAILED, CONFIRMED, etc.) so the member
     * can see their full history, including failed payment attempts.
     *
     * @Transactional(readOnly = true) is required because Registration.event is
     * FetchType.LAZY and we access event.getTitle() etc. during mapping.
     */
    @Transactional(readOnly = true)
    public List<MyBookingResponse> getMyBookings(String sessionToken) {

        // 1. Resolve session — throws 401 if expired or invalid
        MemberSession session = getSessionOrThrow(sessionToken);

        // 2. Fetch all registrations for this member, newest first
        List<Registration> registrations = registrationRepository
                .findByMemberIdOrderByBookedAtDesc(session.getMemberId());

        log.info("My Bookings: member {} has {} registration(s)", session.getMemberId(), registrations.size());

        // 3. Map to member-facing DTO
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
                // Event fields — safe to access inside @Transactional
                .eventTitle(r.getEvent().getTitle())
                .eventDate(r.getEvent().getEventDate())
                .eventStartTime(r.getEvent().getStartTime())
                .eventVenue(r.getEvent().getVenue())
                .eventUniqueLink(r.getEvent().getUniqueEventLink())
                .bookedAt(r.getBookedAt())
                .build();
    }
}

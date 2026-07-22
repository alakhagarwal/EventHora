package com.eventHora.backend.controller;

import com.eventHora.backend.dto.ConfirmPaymentRequest;
import com.eventHora.backend.dto.InitiateBookingRequest;
import com.eventHora.backend.dto.InitiateBookingResponse;
import com.eventHora.backend.dto.MyBookingResponse;
import com.eventHora.backend.dto.RegistrationResponse;
import com.eventHora.backend.dto.VerifyMemberRequest;
import com.eventHora.backend.dto.VerifyMemberResponse;
import com.eventHora.backend.dto.VerifyOtpRequest;
import com.eventHora.backend.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * POST /api/registration/verify-member
     *
     * Validates member ID + identifier with the RIC API and creates a Redis Session.
     * Returns a session token and the member's masked details.
     *
     * Access: PUBLIC
     */
    @PostMapping("/verify-member")
    public ResponseEntity<VerifyMemberResponse> verifyMember(@Valid @RequestBody VerifyMemberRequest request) {
        return ResponseEntity.ok(registrationService.verifyMember(request));
    }

    /**
     * POST /api/registration/initiate
     *
     * Validates all booking rules (capacity, deadline, duplicates, per-member quota).
     * Generates a 6-digit OTP, locks the BookingIntent in Redis, and "sends" the OTP
     * (console-logged for now — swap in WhatsApp/Email provider when keys are ready).
     *
     * Access: PUBLIC (guarded internally by sessionToken)
     */
    @PostMapping("/initiate")
    public ResponseEntity<InitiateBookingResponse> initiateBooking(@Valid @RequestBody InitiateBookingRequest request) {
        return ResponseEntity.ok(registrationService.initiateBooking(request));
    }

    /**
     * POST /api/registration/verify-otp
     *
     * Verifies the 6-digit OTP and finalizes the booking in Postgres.
     * Returns one of three outcomes based on payment path:
     *   - FREE:        Booking confirmed immediately (free event).
     *   - PAY_AT_GATE: Seat reserved, payment collected at the venue.
     *   - PENDING:     Razorpay order created; frontend must open the payment popup.
     *
     * Access: PUBLIC (guarded internally by sessionToken + OTP)
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<RegistrationResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(registrationService.verifyOtpAndBook(request));
    }

    /**
     * POST /api/registration/confirm-payment
     *
     * Called by the frontend immediately after the Razorpay JS popup closes
     * with a successful payment.
     *
     * The frontend sends the three values from the Razorpay JS SDK callback:
     *   - razorpayOrderId   (from our /verify-otp response)
     *   - razorpayPaymentId (from Razorpay JS after payment)
     *   - razorpaySignature (from Razorpay JS after payment)
     *
     * The backend verifies the cryptographic signature, handles the sold-out
     * race condition, and confirms the booking.
     *
     * Access: PUBLIC (secured internally by Razorpay signature verification)
     */
    @PostMapping("/confirm-payment")
    public ResponseEntity<RegistrationResponse> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(registrationService.confirmPayment(request));
    }

    /**
     * GET /api/registration/my-bookings?sessionToken={token}
     *
     * Returns the calling member's complete booking history across all events.
     * Results are ordered newest first (most recent booking at index 0).
     *
     * The sessionToken is required and is resolved to a memberId server-side.
     * The memberId is NEVER accepted directly as a parameter — the session is the
     * only trusted source to prevent a member from viewing another member's bookings.
     *
     * All statuses are included (CONFIRMED, FREE, PAY_AT_GATE, PENDING, FAILED, COMPLIMENTARY)
     * so the member can see their full history, not just successful bookings.
     *
     * Access: PUBLIC (guarded by sessionToken)
     */
    @GetMapping("/my-bookings")
    public ResponseEntity<List<MyBookingResponse>> getMyBookings(
            @RequestParam String sessionToken) {
        return ResponseEntity.ok(registrationService.getMyBookings(sessionToken));
    }
}


package com.eventHora.backend.controller;

import com.eventHora.backend.dto.InitiateBookingRequest;
import com.eventHora.backend.dto.InitiateBookingResponse;
import com.eventHora.backend.dto.RegistrationResponse;
import com.eventHora.backend.dto.VerifyMemberRequest;
import com.eventHora.backend.dto.VerifyMemberResponse;
import com.eventHora.backend.dto.VerifyOtpRequest;
import com.eventHora.backend.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * Validates the OTP entered by the member, creates the Registration record,
     * and returns the ticket reference and payment details.
     *
     * Access: PUBLIC (guarded internally by sessionToken + OTP)
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<RegistrationResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(registrationService.verifyOtp(request));
    }
}


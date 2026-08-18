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

    @PostMapping("/verify-member")
    public ResponseEntity<VerifyMemberResponse> verifyMember(@Valid @RequestBody VerifyMemberRequest request) {
        return ResponseEntity.ok(registrationService.verifyMember(request));
    }

    @PostMapping("/initiate")
    public ResponseEntity<InitiateBookingResponse> initiateBooking(@Valid @RequestBody InitiateBookingRequest request) {
        return ResponseEntity.ok(registrationService.initiateBooking(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<RegistrationResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(registrationService.verifyOtpAndBook(request));
    }

    @PostMapping("/confirm-payment")
    public ResponseEntity<RegistrationResponse> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(registrationService.confirmPayment(request));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<MyBookingResponse>> getMyBookings(
            @RequestParam String sessionToken) {
        return ResponseEntity.ok(registrationService.getMyBookings(sessionToken));
    }
}

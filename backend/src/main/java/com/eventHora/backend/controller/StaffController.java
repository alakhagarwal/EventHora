package com.eventHora.backend.controller;

import com.eventHora.backend.dto.CheckInRequest;
import com.eventHora.backend.dto.CheckInResponse;
import com.eventHora.backend.dto.RecordPaymentRequest;
import com.eventHora.backend.dto.RegistrationSummaryResponse;
import com.eventHora.backend.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final RegistrationService registrationService;

    @PostMapping("/checkin")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(registrationService.checkIn(request));
    }

    @PostMapping("/record-payment")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CheckInResponse> recordPayment(@Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(registrationService.recordGatePayment(request));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<RegistrationSummaryResponse> lookupTicket(
            @RequestParam String ticketReference) {
        return ResponseEntity.ok(registrationService.lookupByTicketReference(ticketReference));
    }
}

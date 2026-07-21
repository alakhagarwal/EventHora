package com.eventHora.backend.controller;

import com.eventHora.backend.dto.CheckInRequest;
import com.eventHora.backend.dto.CheckInResponse;
import com.eventHora.backend.dto.RecordPaymentRequest;
import com.eventHora.backend.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for event-day gate operations performed by STAFF (or ADMIN).
 *
 * All endpoints require a valid JWT with STAFF or ADMIN role.
 * Base path: /api/staff
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final RegistrationService registrationService;

    /**
     * POST /api/staff/checkin
     *
     * Called when a STAFF member scans a member's QR code at the gate.
     *
     * The ticketReference is embedded in the QR code on the member's ticket.
     *
     * Outcomes:
     *  - 200 OK, alreadyCheckedIn=false  → First-time scan, member admitted ✅
     *  - 200 OK, alreadyCheckedIn=true   → Ticket was already scanned (duplicate) ⚠️
     *  - 404 Not Found                   → Ticket reference does not exist
     *  - 409 Conflict                    → PENDING payment (incomplete) or FAILED payment
     *
     * Access: STAFF, ADMIN
     */
    @PostMapping("/checkin")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(registrationService.checkIn(request));
    }

    /**
     * POST /api/staff/record-payment
     *
     * Records cash or complimentary payment for a PAY_AT_GATE ticket and
     * simultaneously checks the member in. These two steps are atomic —
     * staff does NOT need to do a separate QR scan after recording payment.
     *
     * Actions:
     *  - "PAID"          → member paid cash; status becomes CONFIRMED
     *  - "COMPLIMENTARY" → staff waives fee; status becomes COMPLIMENTARY
     *
     * Only tickets with current status PAY_AT_GATE can be processed here.
     *
     * Outcomes:
     *  - 200 OK                → Payment recorded + member admitted ✅
     *  - 400 Bad Request       → Invalid action value
     *  - 404 Not Found         → Ticket reference does not exist
     *  - 409 Conflict          → Ticket is not in PAY_AT_GATE status
     *
     * Access: STAFF, ADMIN
     */
    @PostMapping("/record-payment")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<CheckInResponse> recordPayment(@Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(registrationService.recordGatePayment(request));
    }
}

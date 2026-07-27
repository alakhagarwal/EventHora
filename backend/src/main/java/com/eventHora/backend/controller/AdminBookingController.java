package com.eventHora.backend.controller;

import com.eventHora.backend.dto.AdminBookingRequest;
import com.eventHora.backend.dto.AdminBookingResponse;
import com.eventHora.backend.service.AdminBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for admin-initiated booking operations.
 *
 * These endpoints allow an ADMIN or STAFF member to register a RIC member
 * for an event directly — bypassing the member-facing OTP verification flow.
 *
 * Base path: /api/admin/bookings
 * Access: ADMIN or STAFF (enforced by @PreAuthorize on each method)
 */
@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    /**
     * POST /api/admin/bookings/register
     *
     * Registers a member for an event on their behalf.
     * No OTP, no Razorpay — admin is the trusted actor.
     *
     * Payment path is driven by the `action` field in the request body:
     *  - FREE event (ticketPrice == 0): automatically FREE regardless of `action`
     *  - PAY_AT_GATE: member owes money; staff collects at the gate
     *  - COMPLIMENTARY: fee waived by admin decision; totalAmount = 0.00
     *
     * @param request     The booking details (memberId, eventId, quantity, action)
     * @param userDetails Injected from the JWT — used to capture `bookedBy` for audit
     * @return 200 OK with the created booking details
     *
     * Outcomes:
     *  - 200 OK                → Booking created ✅
     *  - 400 Bad Request       → Invalid memberId, event not PUBLISHED, deadline passed,
     *                            quantity exceeds limit, not enough seats
     *  - 404 Not Found         → Event ID does not exist
     *  - 409 Conflict          → A registration already exists for this member + event
     *  - 401 Unauthorized      → JWT missing or expired
     *  - 403 Forbidden         → Caller does not have ADMIN or STAFF role
     *
     * Access: ADMIN, STAFF
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<AdminBookingResponse> registerMember(
            @Valid @RequestBody AdminBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String bookedByEmail = userDetails.getUsername(); // email is the username in this system
        return ResponseEntity.ok(
                adminBookingService.registerMemberByAdmin(request, bookedByEmail));
    }
}

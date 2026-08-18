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

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<AdminBookingResponse> registerMember(
            @Valid @RequestBody AdminBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String bookedByEmail = userDetails.getUsername();
        return ResponseEntity.ok(
                adminBookingService.registerMemberByAdmin(request, bookedByEmail));
    }
}

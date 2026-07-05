package com.eventHora.backend.controller;

import com.eventHora.backend.dto.*;
import com.eventHora.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * Public — no JWT required.
     * ADMIN and STAFF both log in through this same endpoint.
     * The role inside the returned JWT determines what they can do.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/me
     * Requires: any authenticated user (ADMIN or STAFF).
     * Returns the profile of the currently logged-in system user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * POST /api/auth/users
     * Requires: ADMIN only.
     * Creates a new system user account (ADMIN or STAFF).
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> createUser(
            @Valid @RequestBody CreateStaffRequest request) {
        UserProfileResponse created = authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/auth/users
     * Requires: ADMIN only.
     * Returns a list of all system user accounts.
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /**
     * PATCH /api/auth/users/{email}/deactivate
     * Requires: ADMIN only.
     * Deactivates a user account (soft delete — sets active = false).
     * The user will no longer be able to log in.
     */
    @PatchMapping("/users/{email}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable String email) {
        authService.deactivateUser(email);
        return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
    }
}

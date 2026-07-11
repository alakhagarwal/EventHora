package com.eventHora.backend.controller;

import com.eventHora.backend.dto.VerifyMemberRequest;
import com.eventHora.backend.dto.VerifyMemberResponse;
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
}

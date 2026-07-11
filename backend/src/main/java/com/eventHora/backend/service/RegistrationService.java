package com.eventHora.backend.service;

import com.eventHora.backend.Enum.MemberType;
import com.eventHora.backend.dto.MemberSession;
import com.eventHora.backend.dto.VerifyMemberRequest;
import com.eventHora.backend.dto.VerifyMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // Prefix for session keys in Redis
    private static final String SESSION_PREFIX = "session:";
    // How long the "Soft Login" session lasts before they have to re-verify their Member ID
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    /**
     * Endpoint 1: Verifies the member via RIC API (mocked for now) and creates a Redis Session.
     */
    public VerifyMemberResponse verifyMember(VerifyMemberRequest request) {
        
        // 1. Format Validation based on Member Type
        if (request.getMemberType() == MemberType.INDIAN) {
            // Allows standard 10 digit Indian number, optionally with +91 prefix
            if (!request.getIdentifier().matches("^(\\+91[\\-\\s]?)?[0-9]{10}$")) {
                throw new IllegalArgumentException("Invalid mobile number format. Must be a valid Indian mobile number.");
            }
        } else if (request.getMemberType() == MemberType.OVERSEAS) {
            // Standard basic email validation
            if (!request.getIdentifier().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("Invalid email format.");
            }
        }

        // 2. Call External RIC API (MOCKED)
        boolean isValid = mockRicApi(request.getMemberId(), request.getIdentifier());
        if (!isValid) {
            throw new IllegalArgumentException("Invalid Member ID or Identifier");
        }

        // 2. Generate a secure session token
        String sessionToken = UUID.randomUUID().toString();

        // 3. Save the verified member details in Redis
        MemberSession session = MemberSession.builder()
                .sessionToken(sessionToken)
                .memberId(request.getMemberId())
                .identifier(request.getIdentifier())
                .memberType(request.getMemberType())
                .build();

        redisTemplate.opsForValue().set(SESSION_PREFIX + sessionToken, session, SESSION_TTL);

        // 4. Return the session token and masked identifier to the frontend
        return VerifyMemberResponse.builder()
                .sessionToken(sessionToken)
                .memberId(request.getMemberId())
                .memberType(request.getMemberType())
                .maskedIdentifier(maskIdentifier(request.getIdentifier(), request.getMemberType()))
                .build();
    }

    /**
     * Mock of the external RIC API.
     * Currently returns TRUE if memberId starts with "RIC", otherwise FALSE.
     */
    private boolean mockRicApi(String memberId, String identifier) {
        // In reality, we'd use RestClient to hit their API:
        // return restClient.get().uri("...").retrieve().body(Boolean.class);
        return memberId != null && memberId.toUpperCase().startsWith("RIC");
    }

    /**
     * Helper to mask the phone/email for UX so they know where the OTP will be sent.
     */
    private String maskIdentifier(String identifier, MemberType type) {
        if (identifier == null || identifier.length() < 4) return "****";
        
        if (type == MemberType.INDIAN) {
            // Mask mobile: "98****12"
            return identifier.substring(0, 2) + "****" + identifier.substring(identifier.length() - 2);
        } else {
            // Mask email: "r****@gmail.com"
            int atIndex = identifier.indexOf('@');
            if (atIndex <= 1) return "****" + identifier.substring(atIndex);
            return identifier.charAt(0) + "****" + identifier.substring(atIndex);
        }
    }
}

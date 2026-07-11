package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.MemberType;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for POST /api/registration/verify-member
 *
 * Returned after the RIC API confirms the member exists and their details match.
 *
 * The frontend uses this to:
 *   1. Store the sessionToken, memberId, and memberType in browser state for the next step
 *      (initiating a booking), so they don't have to type it again.
 *
 * (Note: The external RIC API only returns a boolean validity check, so we do not have
 * the member's name or annual fee status).
 */
@Data
@Builder
public class VerifyMemberResponse {
    private String sessionToken;       // Redis session token. Frontend must send this in the next step.
    private String memberId;           // Echoed back so the frontend can cache it
    private MemberType memberType;     // INDIAN or OVERSEAS — determines OTP channel later
    private String maskedIdentifier;   // e.g. "98****12" or "r****@gmail.com" — shown to user
                                       // so they know where their OTP will be sent when they book
}

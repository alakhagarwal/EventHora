package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.MemberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for POST /api/registration/verify-member
 *
 * The frontend sends the Member ID and their registered identifier.
 * The identifier is either:
 *   - a mobile number  (INDIAN members)
 *   - an email address (OVERSEAS members)
 *
 * The backend uses these to call the RIC API and check if they are a valid member.
 * No OTP is sent at this stage — this is just an identity check.
 */
@Data
public class VerifyMemberRequest {

    @NotBlank(message = "Member ID is required")
    private String memberId;           // e.g. "RIC-2023-04512"

    @NotBlank(message = "Identifier is required")
    private String identifier;         // Mobile number (Indian) or Email address (Overseas)

    @NotNull(message = "Member type is required")
    private MemberType memberType;     // INDIAN or OVERSEAS — tells us what OTP channel to use later
}

package com.eventHora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for POST /api/registration/verify-otp
 *
 * The frontend sends the session token and the OTP the member received.
 */
@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Session token is required")
    private String sessionToken;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits")
    private String otp;
}

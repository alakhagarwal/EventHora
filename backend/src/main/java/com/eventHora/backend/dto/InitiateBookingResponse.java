package com.eventHora.backend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for POST /api/registration/initiate
 *
 * Returned when OTP has been generated and "sent" (console-logged for now).
 * The frontend shows a masked message so the user knows where to look.
 */
@Data
@Builder
public class InitiateBookingResponse {

    private String message;          // e.g. "OTP sent to 98****10"
    private int expiresInSeconds;    // Always 300 (5 minutes) — shown as a countdown timer on frontend
}

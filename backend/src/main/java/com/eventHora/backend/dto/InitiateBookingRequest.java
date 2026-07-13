package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentPreference;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO for POST /api/registration/initiate
 *
 * The frontend sends this after the member has been verified (verify-member).
 * The sessionToken ties this request to the verified Redis session.
 */
@Data
public class InitiateBookingRequest {

    @NotBlank(message = "Session token is required")
    private String sessionToken;

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @Min(value = 1, message = "You must book at least 1 ticket")
    private int quantity;

    @NotNull(message = "Payment preference is required")
    private PaymentPreference paymentPreference;  // ONLINE or AT_GATE
}

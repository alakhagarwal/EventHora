package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentPreference;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InitiateBookingRequest {

    @NotBlank(message = "Session token is required")
    private String sessionToken;

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @Min(value = 1, message = "You must book at least 1 ticket")
    private int quantity;

    @NotNull(message = "Payment preference is required")
    private PaymentPreference paymentPreference;
}

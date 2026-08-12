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
 *
 * A member must book at least 1 member ticket (themselves).
 * Guest tickets are optional (0 is valid — attending alone).
 * The backend enforces that guestQuantity <= event.maxGuestTickets.
 */
@Data
public class InitiateBookingRequest {

    @NotBlank(message = "Session token is required")
    private String sessionToken;

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @Min(value = 1, message = "You must book at least 1 member ticket")
    private int memberQuantity;               // RIC-member tier seats (min 1 — the member themselves)

    @Min(value = 0, message = "Guest quantity cannot be negative")
    private int guestQuantity;               // Non-member (guest) seats (0 = attending alone)

    @NotNull(message = "Payment preference is required")
    private PaymentPreference paymentPreference;  // ONLINE or AT_GATE
}

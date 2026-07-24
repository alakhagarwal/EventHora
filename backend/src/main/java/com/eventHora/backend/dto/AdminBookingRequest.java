package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.AdminBookingAction;
import com.eventHora.backend.Enum.MemberType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for POST /api/admin/bookings/register
 *
 * Admin (or STAFF) registers a member for an event without going through the
 * OTP flow. The admin's JWT is the trust anchor — no session token or OTP needed.
 *
 * The `action` field drives the payment path for paid events:
 *   - PAY_AT_GATE  → member still owes money; staff collects at the gate
 *   - COMPLIMENTARY → fee waived; totalAmount set to 0.00
 *
 * For free events (ticketPrice == 0), `action` is ignored and the booking
 * is automatically set to FREE.
 */
@Data
public class AdminBookingRequest {

    @NotBlank(message = "Member ID is required")
    private String memberId;          // e.g. "RIC-2024-04512"

    @NotNull(message = "Member type is required")
    private MemberType memberType;    // INDIAN or OVERSEAS

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @NotNull(message = "Action is required")
    private AdminBookingAction action; // PAY_AT_GATE or COMPLIMENTARY
}

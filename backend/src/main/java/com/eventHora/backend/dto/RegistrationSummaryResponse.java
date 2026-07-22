package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.MemberType;
import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight row DTO for the Admin registration list.
 *
 * Returned by GET /api/admin/events/{eventId}/registrations.
 * One row per booking — maps to one Registration entity.
 *
 * Deliberately lightweight: does not include Razorpay IDs or
 * other internal fields that are not needed in the table view.
 */
@Data
@Builder
public class RegistrationSummaryResponse {

    private UUID registrationId;          // Internal UUID (for admin operations)
    private String ticketReference;       // e.g. "TKT-2026-AB12CD" — user-facing ticket ID
    private String memberId;              // e.g. "RIC-2024-04512"
    private MemberType memberType;        // INDIAN or OVERSEAS
    private int quantity;                 // Number of tickets booked
    private BigDecimal totalAmount;       // Total amount for this booking
    private PaymentStatus paymentStatus;  // Current status: CONFIRMED, FREE, PAY_AT_GATE, etc.
    private PaymentPreference paymentPreference; // How the member chose to pay: ONLINE or PAY_AT_GATE
    private boolean isCheckedIn;          // Whether the member has been scanned at the gate
    private LocalDateTime checkedInAt;   // Gate check-in timestamp — null if not yet checked in
    private LocalDateTime bookedAt;      // When the booking was created
}

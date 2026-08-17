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
 * Dual-tier quantities:
 *   - memberQuantity → RIC-member tier seats (min 1)
 *   - guestQuantity  → Guest (non-member) seats (0 is valid)
 *
 * The `memberContact` field is mandatory and holds the member's contact info:
 *   - INDIAN   → mobile phone number (10-digit)
 *   - OVERSEAS → email address
 * This is used for sending the Razorpay Payment Link when action == ONLINE,
 * and stored on the registration for future communications.
 *
 * The `action` field drives the payment path for paid events:
 *   - PAY_AT_GATE   → member still owes money; staff collects at the gate
 *   - COMPLIMENTARY → fee waived; totalAmount set to 0.00
 *   - ONLINE        → Razorpay Payment Link created and sent to member's phone/email;
 *                     seat IS held while link is active (LINK_PENDING status)
 *
 * For free events (both prices == 0), `action` is ignored and the booking
 * is automatically set to FREE.
 */
@Data
public class AdminBookingRequest {

    @NotBlank(message = "Member ID is required")
    private String memberId;           // e.g. "RIC-2024-04512"

    @NotNull(message = "Member type is required")
    private MemberType memberType;     // INDIAN or OVERSEAS

    @NotBlank(message = "Member contact is required")
    private String memberContact;      // Phone number (INDIAN) or email address (OVERSEAS)

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @Min(value = 1, message = "Member quantity must be at least 1")
    private int memberQuantity;        // RIC-member tier seats

    @Min(value = 0, message = "Guest quantity cannot be negative")
    private int guestQuantity;         // Guest (non-member) seats (0 = member attending alone)

    @NotNull(message = "Action is required")
    private AdminBookingAction action; // PAY_AT_GATE, COMPLIMENTARY, or ONLINE
}

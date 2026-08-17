package com.eventHora.backend.Enum;

/**
 * The payment actions available when admin books on behalf of a member.
 *
 * Intentionally separate from PaymentPreference (which has ONLINE/AT_GATE for members)
 * because admin bookings have different semantics.
 *
 *   PAY_AT_GATE   → member pays cash/card at the venue on event day
 *   COMPLIMENTARY → fee waived by admin; member attends for free
 *   ONLINE        → admin sends a Razorpay Payment Link to the member's
 *                   phone/email; member pays remotely before the event
 *
 * For free events (both tier prices == 0), this value is ignored and the
 * booking is automatically treated as FREE.
 */
public enum AdminBookingAction {
    PAY_AT_GATE,
    COMPLIMENTARY,
    ONLINE
}

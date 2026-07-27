package com.eventHora.backend.Enum;

/**
 * The two payment actions available when admin books on behalf of a member.
 *
 * Intentionally separate from PaymentPreference (which has ONLINE/AT_GATE for members)
 * because admin bookings never go through Razorpay — there is no ONLINE option.
 *
 *   PAY_AT_GATE   → member will pay cash/card at the venue; totalAmount is retained
 *   COMPLIMENTARY → fee waived by admin; totalAmount is set to 0.00
 *
 * For free events (ticketPrice == 0), this value is ignored and the booking
 * is automatically treated as FREE.
 */
public enum AdminBookingAction {
    PAY_AT_GATE,
    COMPLIMENTARY
}

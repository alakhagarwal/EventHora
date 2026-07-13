package com.eventHora.backend.Enum;

/**
 * Represents the payment lifecycle of a Registration.
 *
 * Flow for FREE events:
 *   → FREE (immediately confirmed, no payment involved)
 *
 * Flow for PAID events (Online):
 *   → PENDING (Razorpay order created, awaiting payment)
 *   → CONFIRMED (Razorpay webhook confirms payment success)
 *   → FAILED (Razorpay webhook reports payment failure/timeout)
 *
 * Flow for PAID events (Pay at Gate):
 *   → PAY_AT_GATE (seat reserved, payment collected at venue)
 *   → COMPLIMENTARY (set by STAFF at gate to waive the payment for a member)
 */
public enum PaymentStatus {
    FREE,           // Event is free — confirmed immediately
    PENDING,        // Online payment initiated via Razorpay, awaiting webhook
    CONFIRMED,      // Online payment successfully confirmed by Razorpay
    FAILED,         // Razorpay payment failed or timed out
    PAY_AT_GATE,    // Member opted to pay at the venue gate
    COMPLIMENTARY   // Staff manually waived the fee at the gate
}

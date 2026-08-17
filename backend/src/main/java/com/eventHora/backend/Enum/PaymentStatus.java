package com.eventHora.backend.Enum;

/**
 * Represents the payment lifecycle of a Registration.
 *
 * Flow for FREE events:
 *   → FREE (immediately confirmed, no payment involved)
 *
 * Flow for PAID events (Member Online via Razorpay):
 *   → PENDING      (Razorpay order created, awaiting payment; seat NOT held)
 *   → CONFIRMED    (Razorpay webhook confirms payment success)
 *   → FAILED       (Razorpay webhook reports payment failure/timeout)
 *
 * Flow for PAID events (Admin — Payment Link):
 *   → LINK_PENDING (Admin created a Razorpay Payment Link; seat IS held while link is active)
 *   → CONFIRMED    (payment_link.paid webhook fires; seat was already held)
 *
 * Flow for PAID events (Admin — Pay at Gate):
 *   → PAY_AT_GATE  (seat reserved, payment collected at venue by staff)
 *
 * Flow for PAID events (Admin — Complimentary):
 *   → COMPLIMENTARY (fee waived by admin; no payment required)
 *
 * Key distinction between PENDING and LINK_PENDING:
 *   PENDING      → member self-service, seat NOT counted in sumLockedTickets()
 *   LINK_PENDING → admin-created payment link, seat IS counted in sumLockedTickets()
 */
public enum PaymentStatus {
    FREE,           // Event is free — confirmed immediately
    PENDING,        // Member self-service: Razorpay order created, awaiting payment. Seat NOT held.
    CONFIRMED,      // Payment successfully confirmed by Razorpay (any path)
    FAILED,         // Razorpay payment failed or timed out
    PAY_AT_GATE,    // Member will pay cash/card at the venue gate (admin-created)
    COMPLIMENTARY,  // Fee waived by admin/staff
    LINK_PENDING    // Admin-created Payment Link: member pays remotely. Seat IS held.
}

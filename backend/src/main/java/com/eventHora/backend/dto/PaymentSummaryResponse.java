package com.eventHora.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Full payment + capacity summary DTO for one event.
 *
 * Returned by GET /api/admin/events/{eventId}/payment-summary.
 *
 * Gives admin a single-glance view of:
 *  - How full the event is (capacity vs locked seats)
 *  - A breakdown of registrations by payment status
 *  - How much revenue has been collected vs how much is still expected at the gate
 *
 * "Locked" seats = CONFIRMED + FREE + PAY_AT_GATE + COMPLIMENTARY
 * PENDING and FAILED are excluded from locked count (they don't hold a seat).
 *
 * IMPORTANT — units:
 *  - Capacity fields (totalCapacity, seatsLocked, seatsRemaining) are in TICKETS.
 *  - Registration count fields (*Count) are in BOOKINGS (one member = one registration, regardless of quantity).
 *  - Check-in fields exist in BOTH units (see below).
 */
@Data
@Builder
public class PaymentSummaryResponse {

    // ─── Capacity (in TICKETS) ─────────────────────────────────────────────────

    private int totalCapacity;            // Total seats configured for the event
    private int seatsLocked;             // Tickets held by locked registrations (CONFIRMED+FREE+PAY_AT_GATE+COMPLIMENTARY)
    private int seatsRemaining;          // totalCapacity - seatsLocked

    // ─── Registration Counts (in BOOKINGS — one per member) ───────────────────

    private long confirmedCount;          // Bookings paid online via Razorpay (CONFIRMED)
    private long payAtGateCount;         // Bookings reserved, cash not yet collected (PAY_AT_GATE)
    private long freeCount;              // Bookings for free events (FREE)
    private long complimentaryCount;     // Bookings where staff waived the fee (COMPLIMENTARY)
    private long pendingCount;           // Bookings with incomplete Razorpay payment (PENDING)
    private long failedCount;            // Bookings where payment failed (FAILED)
    private long totalRegistrations;     // All booking rows in the DB (all statuses combined)

    // ─── Gate Check-In (BOOKINGS level — how many members have arrived) ────────

    private long checkedInCount;         // Number of booking rows where isCheckedIn = true
    private long notCheckedInCount;      // Locked booking rows where member hasn't arrived yet

    // ─── Gate Check-In (TICKETS level — comparable to capacity fields) ─────────

    private long checkedInTickets;       // Sum of quantity for checked-in registrations
    private long notCheckedInTickets;    // seatsLocked - checkedInTickets (seats still expected)

    // ─── Revenue ──────────────────────────────────────────────────────────────

    private BigDecimal totalRevenue;           // Sum of totalAmount for CONFIRMED registrations (online money collected)
    private BigDecimal pendingGateCollection;  // Sum of totalAmount for PAY_AT_GATE registrations (cash not yet collected)
    private BigDecimal complimentaryWaived;    // Sum of totalAmount for COMPLIMENTARY registrations (fees waived)
}

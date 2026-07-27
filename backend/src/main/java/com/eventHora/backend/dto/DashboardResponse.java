package com.eventHora.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Overall platform dashboard snapshot returned by GET /api/admin/dashboard.
 *
 * All metrics are computed at request time — no caching.
 * "This month" means the current calendar month (1st to today).
 *
 * Units:
 *  - *Count fields → number of Registration rows (one per booking)
 *  - *Tickets fields → sum of quantity across registrations (individual seats)
 *  - *Revenue fields → BigDecimal monetary amounts (₹)
 */
@Data
@Builder
public class DashboardResponse {

    // ─── Events Overview ──────────────────────────────────────────────────────

    private long totalEvents;        // All events regardless of status
    private long publishedEvents;    // Currently PUBLISHED (live for registration)
    private long upcomingEvents;     // PUBLISHED and eventDate >= today
    private long draftEvents;        // In DRAFT (not yet live)
    private long completedEvents;    // COMPLETED
    private long cancelledEvents;    // CANCELLED

    // ─── Registrations — All-Time ─────────────────────────────────────────────

    private long totalRegistrations;      // All registration rows (all statuses)
    private long lockedRegistrations;     // CONFIRMED + FREE + PAY_AT_GATE + COMPLIMENTARY
    private long totalTicketsSold;        // Sum of quantity for locked registrations

    // ─── Registrations — This Month ───────────────────────────────────────────

    private long registrationsThisMonth; // All registrations created in the current calendar month
    private long ticketsSoldThisMonth;   // Sum of quantity for locked registrations created this month

    // ─── Revenue — All-Time ───────────────────────────────────────────────────

    private BigDecimal totalRevenue;           // Sum of totalAmount for CONFIRMED registrations (online collected)
    private BigDecimal pendingGateCollection;  // Sum of totalAmount for PAY_AT_GATE (cash not yet collected)
    private BigDecimal complimentaryWaived;    // Sum of totalAmount for COMPLIMENTARY (fees waived)

    // ─── Revenue — This Month ─────────────────────────────────────────────────

    private BigDecimal revenueThisMonth;       // Sum of totalAmount for CONFIRMED registrations booked this month
}

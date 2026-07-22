package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Member-facing booking summary returned by GET /api/registration/my-bookings.
 *
 * One element per registration (one booking per event).
 * Includes enough event context for the member to identify which booking it is
 * without a separate event API call.
 *
 * Intentionally does NOT include internal IDs (Razorpay order/payment IDs,
 * registration UUID) — those are not needed for a member-facing view.
 */
@Data
@Builder
public class MyBookingResponse {

    // ─── Ticket Identity ──────────────────────────────────────────────────────

    private String ticketReference;        // e.g. "TKT-2026-AB12CD" — the member's booking ID
    private int quantity;                  // Number of tickets booked

    // ─── Payment ──────────────────────────────────────────────────────────────

    private BigDecimal totalAmount;        // Total amount charged for this booking
    private PaymentStatus paymentStatus;   // CONFIRMED, FREE, PAY_AT_GATE, COMPLIMENTARY, PENDING, FAILED
    private PaymentPreference paymentPreference; // How the member chose to pay: ONLINE or PAY_AT_GATE

    // ─── Gate Check-In ────────────────────────────────────────────────────────

    private boolean isCheckedIn;           // Has the member been scanned at the gate?
    private LocalDateTime checkedInAt;    // Gate check-in timestamp — null if not yet checked in

    // ─── Event Context (so the member can identify which event this is) ────────

    private String eventTitle;            // e.g. "Mere Mehboob Na Ja…"
    private LocalDate eventDate;          // e.g. 2026-07-08
    private LocalTime eventStartTime;     // e.g. 18:30:00
    private String eventVenue;            // e.g. "Main Audi, RIC"
    private String eventUniqueLink;       // Slug for linking back to the event page

    // ─── Audit ────────────────────────────────────────────────────────────────

    private LocalDateTime bookedAt;       // When the booking was created
}

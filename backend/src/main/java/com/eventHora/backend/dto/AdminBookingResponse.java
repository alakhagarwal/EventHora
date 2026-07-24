package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for POST /api/admin/bookings/register
 *
 * Returned when an admin or staff member successfully registers a member for an event.
 * Includes full booking details + event context + audit info (who booked it).
 *
 * Note: no razorpayOrderId field — admin bookings never go through Razorpay.
 */
@Data
@Builder
public class AdminBookingResponse {

    // ─── Ticket ───────────────────────────────────────────────────────────────

    private String ticketReference;         // e.g. "TKT-2026-AB12CD"
    private int quantity;
    private BigDecimal totalAmount;         // 0.00 if FREE or COMPLIMENTARY
    private PaymentStatus paymentStatus;    // FREE, PAY_AT_GATE, or COMPLIMENTARY

    // ─── Member ───────────────────────────────────────────────────────────────

    private String memberId;               // The member who was booked

    // ─── Event Context ────────────────────────────────────────────────────────

    private String eventTitle;
    private LocalDate eventDate;
    private LocalTime eventStartTime;
    private String eventVenue;

    // ─── Audit ────────────────────────────────────────────────────────────────

    private String bookedBy;               // Email of the admin/staff who created this booking
    private LocalDateTime bookedAt;
}

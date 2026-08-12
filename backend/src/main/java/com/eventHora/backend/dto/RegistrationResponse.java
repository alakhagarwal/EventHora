package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response DTO for POST /api/registration/verify-otp
 *
 * Returned after a booking is successfully finalized.
 *
 * If paymentStatus = FREE / PAY_AT_GATE → show success screen directly.
 * If paymentStatus = PENDING             → razorpayOrderId will be present;
 *                                          frontend opens the Razorpay JS checkout window.
 *
 * Tier breakdown (memberAmount + guestAmount = totalAmount) is included so the
 * success screen and receipt can show an itemised bill.
 */
@Data
@Builder
public class RegistrationResponse {

    private String ticketReference;       // e.g. "TKT-2026-AB12CD"
    private String eventTitle;            // For the success screen

    // ─── Quantity breakdown ───────────────────────────────────────────────────

    private int memberQuantity;           // Member-tier tickets booked
    private int guestQuantity;            // Guest-tier tickets booked
    private int quantity;                 // Total = memberQuantity + guestQuantity

    // ─── Amount breakdown ─────────────────────────────────────────────────────

    private BigDecimal memberAmount;      // Charge for paid member tickets
    private BigDecimal guestAmount;       // Charge for paid guest tickets
    private BigDecimal totalAmount;       // memberAmount + guestAmount

    // ─── Payment ──────────────────────────────────────────────────────────────

    private PaymentStatus paymentStatus;

    // Only present when paymentStatus = PENDING (online payment flow)
    private String razorpayOrderId;
}

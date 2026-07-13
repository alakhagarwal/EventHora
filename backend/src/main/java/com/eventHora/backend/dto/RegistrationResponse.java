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
 */
@Data
@Builder
public class RegistrationResponse {

    private String ticketReference;      // e.g. "TKT-2026-AB12CD"
    private String eventTitle;           // For the success screen
    private int quantity;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;

    // Only present when paymentStatus = PENDING (online payment flow)
    private String razorpayOrderId;
}

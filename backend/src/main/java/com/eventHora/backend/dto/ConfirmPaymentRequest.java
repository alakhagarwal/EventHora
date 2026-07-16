package com.eventHora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for POST /api/registration/confirm-payment
 *
 * The frontend sends these three values back after the user completes payment
 * in the Razorpay popup. All three come directly from the Razorpay JS SDK
 * callback — the frontend must NOT modify them.
 *
 * Razorpay signs (orderId + "|" + paymentId) with your API secret.
 * We re-compute that signature on the backend. If it matches, the payment
 * data is 100% authentic and was not tampered with.
 */
@Data
public class ConfirmPaymentRequest {

    @NotBlank(message = "Ticket reference is required")
    private String ticketReference;      // e.g. "TKT-2026-AB12CD" — our internal identifier

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;      // e.g. "order_PwZa8xyz..." — from verify-otp response

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;    // e.g. "pay_Qx3Rabc..." — from Razorpay JS SDK callback

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;    // HMAC-SHA256 signature — from Razorpay JS SDK callback
}

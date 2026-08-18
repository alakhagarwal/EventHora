package com.eventHora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmPaymentRequest {

    @NotBlank(message = "Ticket reference is required")
    private String ticketReference;

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
}

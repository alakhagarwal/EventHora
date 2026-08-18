package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RegistrationResponse {

    private String ticketReference;
    private String eventTitle;
    private int quantity;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;

    private String razorpayOrderId;
}

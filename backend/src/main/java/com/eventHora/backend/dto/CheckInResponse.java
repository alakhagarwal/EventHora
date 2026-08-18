package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CheckInResponse {

    private String ticketReference;
    private String memberId;
    private String eventTitle;
    private int quantity;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;

    private boolean alreadyCheckedIn;
    private LocalDateTime checkedInAt;

    private String message;
}

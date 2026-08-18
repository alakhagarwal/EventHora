package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AdminBookingResponse {

    private String ticketReference;
    private int quantity;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;

    private String memberId;

    private String eventTitle;
    private LocalDate eventDate;
    private LocalTime eventStartTime;
    private String eventVenue;

    private String bookedBy;
    private LocalDateTime bookedAt;
}

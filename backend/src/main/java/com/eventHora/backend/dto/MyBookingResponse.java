package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.Enum.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class MyBookingResponse {

    private String ticketReference;
    private int quantity;

    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private PaymentPreference paymentPreference;

    @JsonProperty("isCheckedIn")
    private boolean isCheckedIn;
    private LocalDateTime checkedInAt;

    private String eventTitle;
    private LocalDate eventDate;
    private LocalTime eventStartTime;
    private String eventVenue;
    private String eventUniqueLink;

    private LocalDateTime bookedAt;
}

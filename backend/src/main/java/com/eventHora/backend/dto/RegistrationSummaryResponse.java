package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.MemberType;
import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.Enum.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RegistrationSummaryResponse {

    private UUID registrationId;
    private String ticketReference;
    private String memberId;
    private MemberType memberType;
    private int quantity;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private PaymentPreference paymentPreference;
    @JsonProperty("isCheckedIn")
    private boolean isCheckedIn;
    private LocalDateTime checkedInAt;
    private LocalDateTime bookedAt;
}

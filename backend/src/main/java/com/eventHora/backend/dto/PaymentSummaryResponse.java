package com.eventHora.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentSummaryResponse {

    private int totalCapacity;
    private int seatsLocked;
    private int seatsRemaining;

    private long confirmedCount;
    private long payAtGateCount;
    private long freeCount;
    private long complimentaryCount;
    private long pendingCount;
    private long failedCount;
    private long totalRegistrations;

    private long checkedInCount;
    private long notCheckedInCount;

    private long checkedInTickets;
    private long notCheckedInTickets;

    private BigDecimal totalRevenue;
    private BigDecimal pendingGateCollection;
    private BigDecimal complimentaryWaived;
}

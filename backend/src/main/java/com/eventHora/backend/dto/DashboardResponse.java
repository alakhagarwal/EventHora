package com.eventHora.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardResponse {

    private long totalEvents;
    private long publishedEvents;
    private long upcomingEvents;
    private long draftEvents;
    private long completedEvents;
    private long cancelledEvents;

    private long totalRegistrations;
    private long lockedRegistrations;
    private long totalTicketsSold;

    private long registrationsThisMonth;
    private long ticketsSoldThisMonth;

    private BigDecimal totalRevenue;
    private BigDecimal pendingGateCollection;
    private BigDecimal complimentaryWaived;

    private BigDecimal revenueThisMonth;
}

package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.EventCategory;
import com.eventHora.backend.Enum.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class EventSummaryResponse {

    private UUID id;
    private String title;
    private EventCategory category;
    private String bannerUrl;
    private LocalDate eventDate;
    private LocalTime startTime;
    private String venue;
    private EventStatus status;
    private String uniqueEventLink;
    private int totalCapacity;
    private int bookedCount;
    private int availableCount;
    private boolean registrationOpen;
    private boolean isSoldOut;
}

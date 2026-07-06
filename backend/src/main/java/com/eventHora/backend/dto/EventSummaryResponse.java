package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.EventCategory;
import com.eventHora.backend.Enum.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Lightweight event card DTO for list views (admin dashboard, member event browser).
 * Intentionally minimal — avoids fetching heavy fields like description and notes
 * when only showing a list of events.
 *
 * Maps to: GET /api/admin/events  and  GET /api/events (member event list)
 */
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
    private boolean registrationOpen;         // false if deadline passed or event full
}

package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.EventCategory;
import com.eventHora.backend.Enum.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EventResponse {

    private UUID id;
    private String title;
    private String description;
    private EventCategory category;
    private String bannerUrl;

    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime registrationDeadline;

    private String venue;
    private String additionalVenueInfo;

    private int totalCapacity;
    private int bookedCount;
    private int availableCount;
    private int maxTicketsPerMember;
    private int freeTicketsPerRegistration;
    private BigDecimal ticketPrice;

    private BigDecimal platformFeePerTicket;

    private Integer minimumAge;

    private List<String> importantNotes;
    private String contactPersonName;
    private String contactPersonPhone;

    private EventStatus status;
    private String uniqueEventLink;

    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

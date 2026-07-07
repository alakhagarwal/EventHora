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

/**
 * Full event details — returned for ADMIN and STAFF views.
 * Includes internal fields (platformFee, createdBy) not shown to members.
 */
@Data
@Builder
public class EventResponse {

    private UUID id;
    private String title;
    private String description;
    private EventCategory category;
    private String bannerUrl;

    // Schedule
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime registrationDeadline;

    // Venue
    private String venue;
    private String additionalVenueInfo;

    // Capacity
    private int totalCapacity;
    private int bookedCount;                     // How many seats are already taken
    private int availableCount;                  // totalCapacity - bookedCount
    private int maxTicketsPerMember;             // Total tickets per registration (member + anyone they bring)
    private int freeTicketsPerRegistration;       // How many of those are free
    private BigDecimal ticketPrice;               // Unified price per paid ticket (0.00 for free events)

    // Admin-only fields
    private BigDecimal platformFeePerTicket;

    // Rules
    private Integer minimumAge;

    // Notes & Contact
    private List<String> importantNotes;
    private String contactPersonName;
    private String contactPersonPhone;

    // Status & Link
    private EventStatus status;
    private String uniqueEventLink;

    // Audit
    private String createdByName;                // Name of ADMIN who created the event
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

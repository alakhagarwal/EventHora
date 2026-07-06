package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.EventCategory;
import com.eventHora.backend.Enum.SeatingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Public-facing event details — returned for the member landing page.
 *
 * Intentionally excludes:
 *   - platformFeePerTicket (internal billing detail)
 *   - createdBy (internal audit)
 *   - status (always PUBLISHED for this endpoint)
 *   - bookedCount / availableCount (prevents gaming capacity)
 *
 * Maps to: GET /api/events/{uniqueEventLink}
 */
@Data
@Builder
public class PublicEventResponse {

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

    // What the member needs to know about tickets
    private int maxTicketsPerMember;
    private int freeTicketsPerMember;
    private BigDecimal memberTicketPrice;         // "Entry is FREE for members"

    // Guest info
    private boolean guestsAllowed;
    private Integer maxGuestsPerMember;           // "up to 2 guests"
    private BigDecimal guestTicketPrice;          // "₹1000/- per person"

    // Rules
    private Integer minimumAge;                   // "Minimum Age: 18+"

    // Notes shown on event page (bullet points from the invite)
    private List<String> importantNotes;

    // Contact
    private String contactPersonName;
    private String contactPersonPhone;

    // Registration
    private String uniqueEventLink;
    private boolean registrationOpen;             // false when deadline has passed or event is full
}

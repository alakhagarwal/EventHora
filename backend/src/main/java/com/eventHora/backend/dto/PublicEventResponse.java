package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.EventCategory;
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
 *
 * Maps to:
 *   GET /api/events            (public listing)
 *   GET /api/events/{link}     (public detail / booking page)
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

    // Ticket info — same price for everyone in the booking
    private int maxTicketsPerMember;              // Total tickets a member can book (themselves + anyone with them)
    private int freeTicketsPerRegistration;       // How many of those are free
    private BigDecimal ticketPrice;               // Price per paid ticket (0.00 for free events)

    // Rules
    private Integer minimumAge;                   // "Minimum Age: 18+"

    // Notes shown on event page (bullet points from the invite)
    private List<String> importantNotes;

    // Contact
    private String contactPersonName;
    private String contactPersonPhone;

    // Capacity
    private int totalCapacity;                    // Maximum total tickets for the event
    private int availableCount;                   // Remaining seats (totalCapacity - lockedTickets)

    // Registration
    private String uniqueEventLink;
    private boolean registrationOpen;             // false when deadline has passed or event is full
    private boolean isSoldOut;                    // true when booked tickets >= total capacity
}

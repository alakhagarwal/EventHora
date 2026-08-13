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
 * Includes both ticket tiers so the booking form can show:
 *   - How many member tickets they can book and what each costs
 *   - How many guest tickets they can bring and what each costs
 *   - How many free tickets apply per tier
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

    // ─── Schedule ─────────────────────────────────────────────────────────────

    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime registrationDeadline;

    // ─── Venue ────────────────────────────────────────────────────────────────

    private String venue;
    private String additionalVenueInfo;

    // ─── Member Ticket Tier ───────────────────────────────────────────────────

    private int maxMemberTickets;                 // Max member-tier seats per booking (min 1)
    private int freeMemberTickets;                // How many member tickets are free per booking
    private BigDecimal memberTicketPrice;          // Price per paid member ticket (0.00 = free)

    // ─── Guest Ticket Tier ────────────────────────────────────────────────────

    private int maxGuestTickets;                  // Max guest seats per booking (0 = no guests)
    private int freeGuestTickets;                 // How many guest tickets are free per booking
    private BigDecimal guestTicketPrice;           // Price per paid guest ticket

    // ─── Event Rules ──────────────────────────────────────────────────────────

    private Integer minimumAge;

    // ─── Notes & Contact ──────────────────────────────────────────────────────

    private List<String> importantNotes;
    private String contactPersonName;
    private String contactPersonPhone;

    // ─── Media Gallery ────────────────────────────────────────────────────────

    private List<EventMediaDto> media;            // Ordered by sortOrder ASC; photos are presigned

    // ─── Capacity ─────────────────────────────────────────────────────────────

    private int totalCapacity;
    private int availableCount;                   // Remaining seats (totalCapacity - lockedTickets)

    // ─── Registration ─────────────────────────────────────────────────────────

    private String uniqueEventLink;
    private boolean registrationOpen;             // false when deadline has passed or event is full
    private boolean isSoldOut;                    // true when booked tickets >= total capacity
}

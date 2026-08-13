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
 *
 * Maps to:
 *   GET /api/admin/events/{id}
 *   POST /api/events               (create response)
 *   PATCH /api/events/{id}         (update response)
 *   POST /api/events/{id}/banner   (banner upload response)
 *   POST /api/events/{id}/media/*  (media upload responses)
 */
@Data
@Builder
public class EventResponse {

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

    // ─── Capacity ─────────────────────────────────────────────────────────────

    private int totalCapacity;
    private int bookedCount;                      // Locked seats (non-PENDING, non-FAILED)
    private int availableCount;                   // totalCapacity - bookedCount

    // ─── Member Ticket Tier ───────────────────────────────────────────────────

    private int maxMemberTickets;                 // Max member-tier seats per booking
    private int freeMemberTickets;                // How many member tickets are free per booking
    private BigDecimal memberTicketPrice;          // Price per paid member ticket

    // ─── Guest Ticket Tier ────────────────────────────────────────────────────

    private int maxGuestTickets;                  // Max guest seats per booking (0 = no guests allowed)
    private int freeGuestTickets;                 // How many guest tickets are free per booking
    private BigDecimal guestTicketPrice;           // Price per paid guest ticket

    // ─── Admin-only fields ────────────────────────────────────────────────────

    private BigDecimal platformFeePerTicket;       // Applied to all paid tickets (member + guest)

    // ─── Event Rules ──────────────────────────────────────────────────────────

    private Integer minimumAge;

    // ─── Notes & Contact ──────────────────────────────────────────────────────

    private List<String> importantNotes;
    private String contactPersonName;
    private String contactPersonPhone;

    // ─── Media Gallery ────────────────────────────────────────────────────────

    private List<EventMediaDto> media;            // Ordered by sortOrder ASC

    // ─── Status & Link ────────────────────────────────────────────────────────

    private EventStatus status;
    private String uniqueEventLink;

    // ─── Audit ────────────────────────────────────────────────────────────────

    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.EventCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Request DTO for ADMIN partially updating an existing event.
 * All fields are optional — only non-null fields will be applied (PATCH semantics).
 * Maps to: PATCH /api/events/{id}
 */
@Data
public class UpdateEventRequest {

    private String title;
    private String description;
    private EventCategory category;
    private String bannerUrl;

    private LocalDate eventDate;

    private LocalTime startTime;
    private LocalTime endTime;

    // No @Future here — preserves existing deadline when admin doesn't change it.
    // @Future is enforced at creation time in CreateEventRequest.
    private LocalDateTime registrationDeadline;

    private String venue;
    private String additionalVenueInfo;

    // ─── Capacity ─────────────────────────────────────────────────────────────

    @Min(value = 1, message = "Total capacity must be at least 1")
    private Integer totalCapacity;

    // ─── Member Ticket Tier ───────────────────────────────────────────────────

    @Min(value = 1, message = "Max member tickets must be at least 1")
    private Integer maxMemberTickets;

    @Min(value = 0, message = "Free member tickets cannot be negative")
    private Integer freeMemberTickets;

    @DecimalMin(value = "0.0", message = "Member ticket price cannot be negative")
    private BigDecimal memberTicketPrice;

    // ─── Guest Ticket Tier ────────────────────────────────────────────────────

    @Min(value = 0, message = "Max guest tickets cannot be negative")
    private Integer maxGuestTickets;

    @Min(value = 0, message = "Free guest tickets cannot be negative")
    private Integer freeGuestTickets;

    @DecimalMin(value = "0.0", message = "Guest ticket price cannot be negative")
    private BigDecimal guestTicketPrice;

    // ─── Platform Fee ─────────────────────────────────────────────────────────

    @DecimalMin(value = "0.0", message = "Platform fee cannot be negative")
    private BigDecimal platformFeePerTicket;

    // ─── Event Rules ──────────────────────────────────────────────────────────

    @Min(value = 0, message = "Minimum age cannot be negative")
    private Integer minimumAge;

    // ─── Notes & Contact ──────────────────────────────────────────────────────

    private List<String> importantNotes;         // Replaces all existing notes when provided
    private String contactPersonName;
    private String contactPersonPhone;
}

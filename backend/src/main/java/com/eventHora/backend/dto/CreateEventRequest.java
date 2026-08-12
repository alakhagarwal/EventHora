package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.EventCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for ADMIN creating a new event.
 *
 * Dual-tier pricing:
 *  - Member tier: maxMemberTickets / freeMemberTickets / memberTicketPrice
 *  - Guest tier:  maxGuestTickets  / freeGuestTickets  / guestTicketPrice
 *
 * Set maxGuestTickets = 0 to disallow guests entirely.
 * Set memberTicketPrice = 0.00 for a fully-free event (members admitted at no cost).
 */
@Data
public class CreateEventRequest {

    // ─── Basic Info ───────────────────────────────────────────────────────────

    @NotBlank(message = "Event title is required")
    private String title;

    @NotBlank(message = "Event description is required")
    private String description;

    @NotNull(message = "Event category is required")
    private EventCategory category;

    private String bannerUrl;                    // Optional at creation; uploaded via S3 separately

    // ─── Schedule ─────────────────────────────────────────────────────────────

    @NotNull(message = "Event date is required")
    @FutureOrPresent(message = "Event date cannot be in the past")
    private LocalDate eventDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Registration deadline is required")
    @Future(message = "Registration deadline must be in the future")
    private LocalDateTime registrationDeadline;

    // ─── Venue ────────────────────────────────────────────────────────────────

    @NotBlank(message = "Venue is required")
    private String venue;

    private String additionalVenueInfo;          // e.g. "Convention Hall with Lawn" for gala dinner

    // ─── Capacity ─────────────────────────────────────────────────────────────

    @NotNull(message = "Total capacity is required")
    @Min(value = 1, message = "Total capacity must be at least 1")
    private Integer totalCapacity;               // Hard ceiling — member + guest seats combined

    // ─── Member Ticket Tier ───────────────────────────────────────────────────

    @NotNull(message = "Max member tickets is required")
    @Min(value = 1, message = "Max member tickets must be at least 1")
    private Integer maxMemberTickets;            // Max member-tier seats per booking

    @NotNull(message = "Free member tickets is required")
    @Min(value = 0, message = "Free member tickets cannot be negative")
    private Integer freeMemberTickets;           // How many member tickets are free per booking

    @NotNull(message = "Member ticket price is required")
    @DecimalMin(value = "0.0", message = "Member ticket price cannot be negative")
    private BigDecimal memberTicketPrice;        // Price per paid member ticket (0.00 = fully free)

    // ─── Guest Ticket Tier ────────────────────────────────────────────────────

    @NotNull(message = "Max guest tickets is required")
    @Min(value = 0, message = "Max guest tickets cannot be negative")
    private Integer maxGuestTickets;             // Max guest seats per booking (0 = guests not allowed)

    @NotNull(message = "Free guest tickets is required")
    @Min(value = 0, message = "Free guest tickets cannot be negative")
    private Integer freeGuestTickets;            // How many guest tickets are free per booking

    @NotNull(message = "Guest ticket price is required")
    @DecimalMin(value = "0.0", message = "Guest ticket price cannot be negative")
    private BigDecimal guestTicketPrice;         // Price per paid guest ticket (0.00 = guests free)

    // ─── Platform Fee ─────────────────────────────────────────────────────────

    @NotNull(message = "Platform fee is required")
    @DecimalMin(value = "0.0", message = "Platform fee cannot be negative")
    private BigDecimal platformFeePerTicket;     // Applied to all paid tickets (member + guest)

    // ─── Event Rules ──────────────────────────────────────────────────────────

    @Min(value = 0, message = "Minimum age cannot be negative")
    private Integer minimumAge;                  // null = no age restriction

    // ─── Important Notes ──────────────────────────────────────────────────────

    private List<String> importantNotes = new ArrayList<>();

    // ─── Contact ──────────────────────────────────────────────────────────────

    private String contactPersonName;

    private String contactPersonPhone;
}

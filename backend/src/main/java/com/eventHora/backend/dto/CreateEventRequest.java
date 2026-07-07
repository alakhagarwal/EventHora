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
 * All required fields must be present. Optional fields default to sensible values.
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

    // ─── Capacity & Tickets ───────────────────────────────────────────────────

    @NotNull(message = "Total capacity is required")
    @Min(value = 1, message = "Total capacity must be at least 1")
    private Integer totalCapacity;

    @NotNull(message = "Max tickets per member is required")
    @Min(value = 1, message = "Max tickets per member must be at least 1")
    private Integer maxTicketsPerMember;          // Total tickets a member can book (themselves + anyone with them)

    @NotNull(message = "Free tickets per registration is required")
    @Min(value = 0, message = "Free tickets cannot be negative")
    private Integer freeTicketsPerRegistration;   // How many of maxTicketsPerMember are free

    @NotNull(message = "Ticket price is required")
    @DecimalMin(value = "0.0", message = "Ticket price cannot be negative")
    private BigDecimal ticketPrice;               // Unified price per paid ticket (0.00 for fully free events)

    // ─── Platform Fee ─────────────────────────────────────────────────────────

    @NotNull(message = "Platform fee is required")
    @DecimalMin(value = "0.0", message = "Platform fee cannot be negative")
    private BigDecimal platformFeePerTicket;

    // ─── Event Rules ──────────────────────────────────────────────────────────

    @Min(value = 0, message = "Minimum age cannot be negative")
    private Integer minimumAge;                  // null = no age restriction

    // ─── Important Notes ──────────────────────────────────────────────────────

    private List<String> importantNotes = new ArrayList<>();

    // ─── Contact ──────────────────────────────────────────────────────────────

    private String contactPersonName;

    private String contactPersonPhone;
}

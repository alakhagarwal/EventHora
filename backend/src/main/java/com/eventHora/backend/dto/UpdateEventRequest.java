package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.EventCategory;
import com.eventHora.backend.Enum.SeatingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Request DTO for ADMIN partially updating an existing event.
 * All fields are optional — only the non-null fields will be applied.
 * This maps to PATCH /api/events/{id}
 */
@Data
public class UpdateEventRequest {

    private String title;
    private String description;
    private EventCategory category;
    private String bannerUrl;

    @FutureOrPresent(message = "Event date cannot be in the past")
    private LocalDate eventDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @Future(message = "Registration deadline must be in the future")
    private LocalDateTime registrationDeadline;

    private String venue;
    private String additionalVenueInfo;

    @Min(value = 1, message = "Total capacity must be at least 1")
    private Integer totalCapacity;

    @Min(value = 1, message = "Max tickets per member must be at least 1")
    private Integer maxTicketsPerMember;

    @Min(value = 0, message = "Free tickets cannot be negative")
    private Integer freeTicketsPerMember;

    @DecimalMin(value = "0.0", message = "Member ticket price cannot be negative")
    private BigDecimal memberTicketPrice;

    private Boolean guestsAllowed;

    @Min(value = 0, message = "Max guests cannot be negative")
    private Integer maxGuestsPerMember;

    @DecimalMin(value = "0.0", message = "Guest ticket price cannot be negative")
    private BigDecimal guestTicketPrice;

    @DecimalMin(value = "0.0", message = "Platform fee cannot be negative")
    private BigDecimal platformFeePerTicket;

    @Min(value = 0, message = "Minimum age cannot be negative")
    private Integer minimumAge;

    private List<String> importantNotes;         // Replaces all existing notes when provided
    private String contactPersonName;
    private String contactPersonPhone;
}

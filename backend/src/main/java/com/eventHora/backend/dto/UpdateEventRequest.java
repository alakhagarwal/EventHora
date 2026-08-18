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

@Data
public class UpdateEventRequest {

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

    @Min(value = 1, message = "Total capacity must be at least 1")
    private Integer totalCapacity;

    @Min(value = 1, message = "Max tickets per member must be at least 1")
    private Integer maxTicketsPerMember;

    @Min(value = 0, message = "Free tickets cannot be negative")
    private Integer freeTicketsPerRegistration;

    @DecimalMin(value = "0.0", message = "Ticket price cannot be negative")
    private BigDecimal ticketPrice;

    @DecimalMin(value = "0.0", message = "Platform fee cannot be negative")
    private BigDecimal platformFeePerTicket;

    @Min(value = 0, message = "Minimum age cannot be negative")
    private Integer minimumAge;

    private List<String> importantNotes;
    private String contactPersonName;
    private String contactPersonPhone;
}

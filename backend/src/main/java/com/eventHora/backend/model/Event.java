package com.eventHora.backend.model;

import com.eventHora.backend.Enum.EventCategory;
import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.Enum.SeatingType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    @Column
    private String bannerUrl;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private LocalDateTime registrationDeadline;

    @Column(nullable = false)
    private String venue;

    @Column
    private String additionalVenueInfo;

    @Column(nullable = false)
    private int totalCapacity;

    @Column(nullable = false)
    private int maxTicketsPerMember;

    @Column(nullable = false)
    private int freeTicketsPerRegistration;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFeePerTicket;

    @Column
    private Integer minimumAge;

    @ElementCollection
    @CollectionTable(
            name = "event_notes",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @Column(name = "note", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> importantNotes = new ArrayList<>();

    @Column
    private String contactPersonName;

    @Column
    private String contactPersonPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    @Column(unique = true)
    private String uniqueEventLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private SystemUser createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

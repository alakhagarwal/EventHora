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

/**
 * Represents an RIC event (e.g. Musical Evening, Kathak Dance).
 *
 * Pricing model (simplified):
 *  - One unified ticketPrice applies to every ticket (member + any accompanying person).
 *  - freeTicketsPerRegistration defines how many of those are free per booking.
 *  - maxTicketsPerMember is the total a member can book in a single registration
 *    (covering both themselves and anyone they bring along).
 *  - Registration has a hard deadline (registrationDeadline).
 *  - Some events require members to have paid their annual fee.
 *  - importantNotes stores the free-form bullet points admins write in event communications.
 *  - Show and dinner can be at different venues (venue + additionalVenueInfo).
 */
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

    // ─── Basic Info ────────────────────────────────────────────────────────────

    @Column(nullable = false)
    private String title;                         // e.g. "Mere Mehboob Na Ja…"

    @Column(columnDefinition = "TEXT")
    private String description;                   // Full event write-up / invite text

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;               // MUSIC, DANCE, CULTURAL, etc.

    @Column
    private String bannerUrl;                     // S3 URL of event banner/poster image (set via /banner upload)

    // ─── Schedule ─────────────────────────────────────────────────────────────

    @Column(nullable = false)
    private LocalDate eventDate;                  // Date: 08 July 2026

    @Column(nullable = false)
    private LocalTime startTime;                  // Show: 06:30 PM

    @Column(nullable = false)
    private LocalTime endTime;                    // Show ends: 08:00 PM

    @Column(nullable = false)
    private LocalDateTime registrationDeadline;   // Deadline: 03:00 PM of 7 July 2026

    // ─── Venue ────────────────────────────────────────────────────────────────

    @Column(nullable = false)
    private String venue;                         // Primary venue: "Main Audi, RIC"

    @Column
    private String additionalVenueInfo;           // Secondary venue: "Convention Hall with Lawn"
                                                  // Used for gala dinners or post-show activities

    // ─── Capacity & Tickets ───────────────────────────────────────────────────

    @Column(nullable = false)
    private int totalCapacity;                    // Total seats available for the event

    @Column(nullable = false)
    private int maxTicketsPerMember;              // Total tickets per registration (member + anyone they bring)

    @Column(nullable = false)
    private int freeTicketsPerRegistration;       // How many of those maxTicketsPerMember are free
                                                  // e.g. 2 free out of 4 total → pay for 2

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;               // Unified price per paid ticket (same for everyone)
                                                  // 0.00 for fully free events

    // ─── Platform Fee ─────────────────────────────────────────────────────────

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFeePerTicket;     // EventHora fee per paid ticket

    // ─── Event Rules ──────────────────────────────────────────────────────────

    @Column
    private Integer minimumAge;                  // null = no restriction, 18 = 18+

    // ─── Important Notes (free-form bullet points from admin) ─────────────────

    @ElementCollection // A hidden table will be created to store these instead of another table
    @CollectionTable(
            name = "event_notes", // This tells Hibernate to add a foreign key column named event_id to the event_notes table, linking each note back to its parent event.
            joinColumns = @JoinColumn(name = "event_id") //  We are explicitly naming the side-table event_notes
    )
    @Column(name = "note", columnDefinition = "TEXT") // 
    @Builder.Default //  If someone builds an Event but forgets to provide importantNotes, it would normally default to null. By using = new ArrayList<>() combined with @Builder.Default, we guarantee it will always be an empty list instead of a NullPointerException waiting to happen.
    private List<String> importantNotes = new ArrayList<>();
    // e.g. ["Please carry your membership card", "Blocking seats not permitted"]

    // ─── Contact ──────────────────────────────────────────────────────────────

    @Column
    private String contactPersonName;            // e.g. "Mr. Keyur Patel, Marketing Manager"

    @Column
    private String contactPersonPhone;           // e.g. "9462200225"

    // ─── Status & Link ────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING) // so that it is stored as a string DRAFT rather than 0 in database
    @Column(nullable = false)
    @Builder.Default // if not provided, it will be DRAFT by default
    private EventStatus status = EventStatus.DRAFT; 

    @Column(unique = true)
    private String uniqueEventLink;              // UUID slug: eventric.org/e/mere-mehboob-na-ja

    // ─── Audit ────────────────────────────────────────────────────────────────

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

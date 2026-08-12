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
 * Dual-tier pricing model:
 *  - Member tickets  (RIC members)   → maxMemberTickets, freeMemberTickets, memberTicketPrice
 *  - Guest tickets   (non-members)   → maxGuestTickets,  freeGuestTickets,  guestTicketPrice
 *  - totalCapacity is the single hard ceiling combining both tiers.
 *  - A member must book at least 1 member ticket and 0–maxGuestTickets guest tickets.
 *  - Free-ticket quotas are per-booking (not per-event): each registration gets up to
 *    freeMemberTickets member tickets free + freeGuestTickets guest tickets free.
 *  - Registration has a hard deadline (registrationDeadline).
 *  - importantNotes stores the free-form bullet points admins write in event communications.
 *  - Media gallery (photos + videos) is stored in the event_media side-table.
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

    // ─── Capacity ─────────────────────────────────────────────────────────────

    @Column(nullable = false)
    private int totalCapacity;                    // Hard ceiling — total seats (member + guest combined)

    // ─── Member Ticket Tier (RIC members) ────────────────────────────────────

    @Column(nullable = false)
    private int maxMemberTickets;                 // Max member-tier seats per booking (min 1)

    @Column(nullable = false)
    private int freeMemberTickets;                // How many member tickets are free per booking
                                                  // e.g. 2 free → first 2 member tickets cost ₹0

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal memberTicketPrice;         // Price per paid member ticket (0.00 = fully free)

    // ─── Guest Ticket Tier (non-members) ─────────────────────────────────────

    @Column(nullable = false)
    private int maxGuestTickets;                  // Max guest seats per booking (0 = guests not allowed)

    @Column(nullable = false)
    private int freeGuestTickets;                 // How many guest tickets are free per booking

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal guestTicketPrice;          // Price per paid guest ticket (0.00 = guests are free)

    // ─── Platform Fee ─────────────────────────────────────────────────────────

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFeePerTicket;      // EventHora fee per paid ticket (member or guest)

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

    // ─── Media Gallery ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<EventMedia> media = new ArrayList<>();
    // Photos → S3 (presigned URL at read time)   Videos → external embed URL

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

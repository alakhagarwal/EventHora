package com.eventHora.backend.model;

import com.eventHora.backend.Enum.MemberType;
import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.Enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single booking made by an RIC member for a specific event.
 *
 * One Registration = one member booking N tickets for one event.
 *
 * Seat reservation model:
 *  - A seat is considered "taken" when paymentStatus is CONFIRMED, FREE, PAY_AT_GATE, or COMPLIMENTARY.
 *  - PENDING seats do NOT count against capacity (prevents ghost reservations from incomplete payments).
 *  - FAILED registrations are ignored entirely.
 */
@Entity
@Table(
    name = "registrations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_member_event",
        columnNames = {"member_id", "event_id"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ─── Who ──────────────────────────────────────────────────────────────────

    @Column(name = "member_id", nullable = false)
    private String memberId;               // RIC Member ID e.g. "RIC-2024-04512"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberType memberType;         // INDIAN or OVERSEAS — determines OTP channel

    // ─── What ─────────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;                   // The event being booked

    @Column(nullable = false)
    private int quantity;                  // Number of tickets booked (1 to maxTicketsPerMember)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;        // Final amount charged: paidTickets * ticketPrice

    // ─── Payment ──────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;   // Current payment state

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPreference paymentPreference; // How the user chose to pay

    @Column
    private String razorpayOrderId;        // Razorpay order ID — null for free/at-gate bookings

    // ─── Ticket ───────────────────────────────────────────────────────────────

    @Column(nullable = false, unique = true)
    private String ticketReference;        // User-facing ticket ID e.g. "TKT-2026-AB12CD"

    // ─── Audit ────────────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime bookedAt;

    @PrePersist
    protected void onCreate() {
        bookedAt = LocalDateTime.now();
    }
}

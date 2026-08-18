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

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberType memberType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPreference paymentPreference;

    @Column
    private String razorpayOrderId;

    @Column
    private String razorpayPaymentId;

    @Column(name = "is_checked_in", nullable = false)
    @Builder.Default
    private boolean isCheckedIn = false;

    @Column
    private LocalDateTime checkedInAt;

    @Column(nullable = false, unique = true)
    private String ticketReference;

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @PrePersist
    protected void onCreate() {
        bookedAt = LocalDateTime.now();
    }
}

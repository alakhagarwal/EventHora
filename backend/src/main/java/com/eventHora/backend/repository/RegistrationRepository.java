package com.eventHora.backend.repository;

import com.eventHora.backend.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    /**
     * Sums the quantity of all "locked" registrations for a given event.
     *
     * CONFIRMED, FREE, PAY_AT_GATE and COMPLIMENTARY all hold a real seat.
     * PENDING is intentionally excluded — an incomplete Razorpay payment
     * should not block other members from booking.
     * FAILED is excluded — the seat is effectively released.
     */
    @Query(
        value = """
            SELECT COALESCE(SUM(r.quantity), 0)
            FROM registrations r
            WHERE r.event_id = :eventId
              AND r.payment_status IN ('CONFIRMED', 'FREE', 'PAY_AT_GATE', 'COMPLIMENTARY')
            """,
        nativeQuery = true
    )
    int sumLockedTicketsForEvent(@Param("eventId") UUID eventId);

    /**
     * Returns the existing registration for a member + event combination.
     * Used to prevent duplicate bookings.
     */
    Optional<Registration> findByMemberIdAndEventId(String memberId, UUID eventId);

    /**
     * Looks up a registration by the user-facing ticket reference.
     * Used by /confirm-payment and the webhook to finalize a PENDING booking.
     */
    Optional<Registration> findByTicketReference(String ticketReference);

    /**
     * Looks up a registration by the Razorpay order ID.
     * Used by the webhook to find a PENDING booking when the frontend fails to call /confirm-payment.
     */
    Optional<Registration> findByRazorpayOrderId(String razorpayOrderId);

    // ─── Phase 7A: Admin Registration List ────────────────────────────────────

    /**
     * Returns all registrations for a given event, ordered by booking time (newest first).
     * Used by GET /api/admin/events/{eventId}/registrations.
     */
    List<Registration> findByEventIdOrderByBookedAtDesc(UUID eventId);

    // ─── Phase 7B: Admin Payment Summary ──────────────────────────────────────

    /**
     * Returns aggregate payment data per status for a given event.
     *
     * Each row is an Object[] with:
     *   [0] payment_status    (String)
     *   [1] registrationCount (Long)      — number of Registration rows for this status
     *   [2] ticketCount       (Long)      — sum of quantity (total tickets, not just bookings)
     *   [3] totalAmount       (BigDecimal)— sum of totalAmount for this status
     *
     * Note: Using nativeQuery=true so we can alias columns clearly.
     * SUM(quantity) is used as ticketCount because each Registration can book N tickets.
     */
    @Query(
        value = """
            SELECT
                r.payment_status                    AS paymentStatus,
                COUNT(r.id)                         AS registrationCount,
                COALESCE(SUM(r.quantity), 0)        AS ticketCount,
                COALESCE(SUM(r.total_amount), 0.00) AS totalAmount
            FROM registrations r
            WHERE r.event_id = :eventId
            GROUP BY r.payment_status
            """,
        nativeQuery = true
    )
    List<Object[]> getPaymentAggregatesByEventId(@Param("eventId") UUID eventId);

    /**
     * Counts how many REGISTRATION ROWS for an event have been checked in.
     * "How many members (bookings) are at the venue."
     * Only counts locked statuses — excludes PENDING/FAILED.
     *
     * Note: One registration may cover multiple tickets (quantity > 1).
     * Use sumCheckedInTicketsForEvent() when you need ticket-level counts.
     */
    @Query(
        value = """
            SELECT COUNT(r.id)
            FROM registrations r
            WHERE r.event_id = :eventId
              AND r.is_checked_in = true
              AND r.payment_status IN ('CONFIRMED', 'FREE', 'PAY_AT_GATE', 'COMPLIMENTARY')
            """,
        nativeQuery = true
    )
    long countCheckedInForEvent(@Param("eventId") UUID eventId);

    /**
     * Sums the QUANTITY of tickets for checked-in registrations.
     * "How many individual tickets have entered the venue."
     *
     * This is the correct value to subtract from seatsLocked to get
     * notCheckedInCount, because seatsLocked is also in tickets (SUM of quantity).
     */
    @Query(
        value = """
            SELECT COALESCE(SUM(r.quantity), 0)
            FROM registrations r
            WHERE r.event_id = :eventId
              AND r.is_checked_in = true
              AND r.payment_status IN ('CONFIRMED', 'FREE', 'PAY_AT_GATE', 'COMPLIMENTARY')
            """,
        nativeQuery = true
    )
    long sumCheckedInTicketsForEvent(@Param("eventId") UUID eventId);
}

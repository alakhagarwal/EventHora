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

    Optional<Registration> findByMemberIdAndEventId(String memberId, UUID eventId);

    Optional<Registration> findByTicketReference(String ticketReference);

    Optional<Registration> findByRazorpayOrderId(String razorpayOrderId);

    List<Registration> findByMemberIdOrderByBookedAtDesc(String memberId);

    List<Registration> findByEventIdOrderByBookedAtDesc(UUID eventId);

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

    @Query(
        value = """
            SELECT
                r.payment_status                    AS paymentStatus,
                COUNT(r.id)                         AS registrationCount,
                COALESCE(SUM(r.quantity), 0)        AS ticketCount,
                COALESCE(SUM(r.total_amount), 0.00) AS totalAmount
            FROM registrations r
            GROUP BY r.payment_status
            """,
        nativeQuery = true
    )
    List<Object[]> getGlobalPaymentAggregates();

    @Query(
        value = """
            SELECT
                r.payment_status                    AS paymentStatus,
                COUNT(r.id)                         AS registrationCount,
                COALESCE(SUM(r.quantity), 0)        AS ticketCount,
                COALESCE(SUM(r.total_amount), 0.00) AS totalAmount
            FROM registrations r
            WHERE r.booked_at >= :startOfMonth
            GROUP BY r.payment_status
            """,
        nativeQuery = true
    )
    List<Object[]> getMonthlyPaymentAggregates(@Param("startOfMonth") java.time.LocalDateTime startOfMonth);

    @Query(
        value = """
            SELECT r.*
            FROM registrations r
            WHERE r.payment_status = 'PENDING'
              AND r.booked_at < :cutoff
            """,
        nativeQuery = true
    )
    List<Registration> findStalePendingRegistrations(@Param("cutoff") java.time.LocalDateTime cutoff);
}

package com.eventHora.backend.repository;

import com.eventHora.backend.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}

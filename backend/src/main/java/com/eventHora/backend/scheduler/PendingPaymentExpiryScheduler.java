package com.eventHora.backend.scheduler;

import com.eventHora.backend.Enum.PaymentStatus;
import com.eventHora.backend.model.Registration;
import com.eventHora.backend.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Expires stale PENDING registrations by marking them as FAILED.
 *
 * WHY: When a member opens the Razorpay payment popup and then abandons it
 * (closes the app, browser crash, navigates away), Razorpay does NOT send a
 * failure webhook. The registration stays forever in PENDING status, which:
 *   1. Confuses the member — their booking looks "stuck".
 *   2. Does NOT lock a seat (PENDING is excluded from capacity math), but
 *      the stale row blocks that member from creating a new booking for the
 *      same event via the "retry" path (which checks for PENDING/FAILED rows).
 *      In practice, the code DOES allow retry over PENDING, but having a clean
 *      FAILED status makes the member's history clearer and prevents edge cases.
 *
 * WHEN: Runs every 10 minutes.
 *   - This is frequent enough to be useful (stale rows expire within 40 min max)
 *     without being so aggressive it causes DB load.
 *
 * WINDOW: A PENDING registration older than `scheduler.pending-expiry.minutes`
 * (default: 30 minutes) is considered stale. Razorpay orders are valid for
 * 15 minutes, so 30 minutes is a safe buffer.
 *
 * WHAT it does:
 *   PENDING + bookedAt < (now - expiryMinutes) → FAILED
 *
 * WHAT it does NOT touch:
 *   - CONFIRMED, FREE, PAY_AT_GATE, COMPLIMENTARY: Already settled, not PENDING.
 *   - Fresh PENDING rows (within the expiry window): Left alone so live payments
 *     still in-flight are not disrupted.
 *
 * IDEMPOTENCY: Safe — FAILED rows are excluded by the query's
 * `payment_status = 'PENDING'` filter.
 *
 * NOTE: This scheduler does NOT attempt to cancel the Razorpay order. Razorpay
 * orders auto-expire after 15 minutes regardless. We only update our own DB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingPaymentExpiryScheduler {

    private final RegistrationRepository registrationRepository;

    /**
     * Injected from application.properties:
     *   scheduler.pending-expiry.minutes=30
     */
    @Value("${scheduler.pending-expiry.minutes:30}")
    private int expiryMinutes;

    /**
     * Runs every 10 minutes.
     * fixedRate is in milliseconds: 10 * 60 * 1000 = 600_000 ms.
     *
     * Using fixedRate (not cron) so it runs 10 minutes after app start,
     * then every 10 minutes — no need to align to a clock boundary.
     */
    @Scheduled(fixedRate = 600_000)
    @Transactional
    public void expireStalePendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expiryMinutes);

        List<Registration> stale = registrationRepository.findStalePendingRegistrations(cutoff);

        if (stale.isEmpty()) {
            log.debug("[SCHEDULER][PendingExpiry] No stale PENDING registrations found (cutoff={}).", cutoff);
            return;
        }

        log.info("[SCHEDULER][PendingExpiry] Found {} stale PENDING registration(s) older than {} minutes. Expiring...",
                stale.size(), expiryMinutes);

        for (Registration reg : stale) {
            log.info("[SCHEDULER][PendingExpiry] Expiring ticket={}, member={}, event={}, bookedAt={}",
                    reg.getTicketReference(),
                    reg.getMemberId(),
                    reg.getEvent().getId(),
                    reg.getBookedAt());
            reg.setPaymentStatus(PaymentStatus.FAILED);
        }

        registrationRepository.saveAll(stale);

        log.info("[SCHEDULER][PendingExpiry] Done. {} registration(s) expired to FAILED.", stale.size());
    }
}

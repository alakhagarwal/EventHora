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

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingPaymentExpiryScheduler {

    private final RegistrationRepository registrationRepository;

    @Value("${scheduler.pending-expiry.minutes:30}")
    private int expiryMinutes;

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

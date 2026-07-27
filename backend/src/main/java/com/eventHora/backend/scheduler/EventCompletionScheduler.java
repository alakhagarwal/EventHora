package com.eventHora.backend.scheduler;

import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.model.Event;
import com.eventHora.backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Automatically marks PUBLISHED events as COMPLETED once their event date has passed.
 *
 * WHY: Admins sometimes forget (or don't have time) to manually complete events
 * after they conclude. Without auto-completion, the dashboard's `completedEvents`
 * count stays permanently at 0, and the events continue showing as "live" in
 * PUBLISHED state even though they happened weeks ago.
 *
 * WHEN: Runs every night at 00:15 AM (15 minutes after midnight).
 *   - 00:15 rather than exactly midnight gives DB/server a soft buffer after a
 *     potential midnight burst of other system tasks.
 *
 * WHAT it transitions:
 *   PUBLISHED + eventDate < today → COMPLETED
 *
 * WHAT it intentionally does NOT touch:
 *   - CANCELLED events:  A cancelled event that happened to pass its date should
 *     remain CANCELLED — it was never held.
 *   - DRAFT events:      Should never have a past date, but even if they do, we
 *     don't auto-complete drafts — they were never published.
 *   - Already COMPLETED: No-op (query already excludes them via status = PUBLISHED).
 *
 * IDEMPOTENCY: Safe to run multiple times. Once an event is COMPLETED, the
 * query no longer returns it (status filter blocks it).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCompletionScheduler {

    private final EventRepository eventRepository;

    /**
     * Cron expression: "15 0 * * *" = every day at 00:15 AM (server time).
     *
     * Spring cron format: second minute hour day-of-month month day-of-week
     *   0  15  0  *  *  *
     */
    @Scheduled(cron = "0 15 0 * * *")
    @Transactional
    public void autoCompleteEvents() {
        LocalDate today = LocalDate.now();

        List<Event> pastPublishedEvents = eventRepository.findPublishedEventsBeforeDate(today);

        if (pastPublishedEvents.isEmpty()) {
            log.info("[SCHEDULER][EventCompletion] No PUBLISHED events found past their date. Nothing to complete.");
            return;
        }

        log.info("[SCHEDULER][EventCompletion] Found {} PUBLISHED event(s) past their date. Marking as COMPLETED...",
                pastPublishedEvents.size());

        for (Event event : pastPublishedEvents) {
            log.info("[SCHEDULER][EventCompletion] Completing event '{}' (id={}, date={})",
                    event.getTitle(), event.getId(), event.getEventDate());
            event.setStatus(EventStatus.COMPLETED);
        }

        // saveAll batches the UPDATE statements into a single transaction
        eventRepository.saveAll(pastPublishedEvents);

        log.info("[SCHEDULER][EventCompletion] Done. {} event(s) marked as COMPLETED.",
                pastPublishedEvents.size());
    }
}

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

@Slf4j
@Component
@RequiredArgsConstructor
public class EventCompletionScheduler {

    private final EventRepository eventRepository;

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

        eventRepository.saveAll(pastPublishedEvents);

        log.info("[SCHEDULER][EventCompletion] Done. {} event(s) marked as COMPLETED.",
                pastPublishedEvents.size());
    }
}

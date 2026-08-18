package com.eventHora.backend.repository;

import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findAllByOrderByEventDateDesc();
    List<Event> findByStatusOrderByEventDateDesc(EventStatus status);
    Optional<Event> findByUniqueEventLink(String uniqueEventLink);
    boolean existsByUniqueEventLink(String uniqueEventLink);

    @Query(
        value = """
            SELECT e.status AS status, COUNT(e.id) AS cnt
            FROM events e
            GROUP BY e.status
            """,
        nativeQuery = true
    )
    List<Object[]> countEventsByStatus();

    @Query(
        value = """
            SELECT COUNT(e.id)
            FROM events e
            WHERE e.status = 'PUBLISHED'
              AND e.event_date >= :today
            """,
        nativeQuery = true
    )
    long countUpcomingEvents(@Param("today") LocalDate today);

    @Query(
        value = """
            SELECT e.*
            FROM events e
            WHERE e.status = 'PUBLISHED'
              AND e.event_date < :today
            """,
        nativeQuery = true
    )
    List<Event> findPublishedEventsBeforeDate(@Param("today") LocalDate today);
}

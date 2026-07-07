package com.eventHora.backend.repository;

import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findAllByOrderByEventDateDesc();
    List<Event> findByStatusOrderByEventDateDesc(EventStatus status);
    Optional<Event> findByUniqueEventLink(String uniqueEventLink);
    boolean existsByUniqueEventLink(String uniqueEventLink);
}

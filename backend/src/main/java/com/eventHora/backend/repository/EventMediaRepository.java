package com.eventHora.backend.repository;

import com.eventHora.backend.model.EventMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventMediaRepository extends JpaRepository<EventMedia, UUID> {
    
    List<EventMedia> findByEventIdOrderBySortOrderAsc(UUID eventId);
    
    Optional<EventMedia> findByIdAndEventId(UUID id, UUID eventId);
}

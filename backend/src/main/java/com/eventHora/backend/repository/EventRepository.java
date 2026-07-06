package com.eventHora.backend.repository;

import com.eventHora.backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    // Custom query methods can be defined here if needed
}

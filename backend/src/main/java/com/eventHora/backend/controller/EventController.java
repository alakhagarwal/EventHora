package com.eventHora.backend.controller;

import com.eventHora.backend.dto.*;
import com.eventHora.backend.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * POST /api/events
     * Creates a new event in DRAFT status.
     * Access: ADMIN only
     */
    @PostMapping("/api/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        EventResponse response = eventService.createEvent(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PATCH /api/events/{id}
     * Partially updates an existing event. Only fields present in the request body are changed.
     * Access: ADMIN only
     */
    @PatchMapping("/api/events/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request) {

        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    /**
     * PATCH /api/events/{id}/publish
     * Transitions an event from DRAFT to PUBLISHED, making it visible to members.
     * Access: ADMIN only
     */
    @PatchMapping("/api/events/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> publishEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.publishEvent(id));
    }

    /**
     * DELETE /api/events/{id}
     * Cancels an event (sets status to CANCELLED). Does not hard-delete from DB.
     * Access: ADMIN only
     */
    @DeleteMapping("/api/events/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> cancelEvent(@PathVariable UUID id) {
        eventService.cancelEvent(id);
        return ResponseEntity.ok(Map.of("message", "Event cancelled successfully"));
    }

    /**
     * GET /api/admin/events
     * Returns a summary list of all events (all statuses), ordered by event date desc.
     * Access: ADMIN only
     */
    @GetMapping("/api/admin/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventSummaryResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /**
     * GET /api/admin/events/{id}
     * Returns the full details of a single event regardless of its status.
     * Use this before making a PATCH update call so you know the current values.
     * Access: ADMIN only
     */
    @GetMapping("/api/admin/events/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    /**
     * POST /api/events/{id}/banner
     * Uploads a banner image for an event to S3.
     * The returned S3 URL is saved to the event's bannerUrl field.
     * If a banner already exists it is deleted from S3 before uploading the new one.
     *
     * Request: multipart/form-data  key="file"
     * Access: ADMIN only
     */
    @PostMapping(value = "/api/events/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> uploadBanner(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(eventService.uploadBanner(id, file));
    }

    /**
     * GET /api/events
     * Returns a summary list of all PUBLISHED events.
     * Access: PUBLIC
     */
    @GetMapping("/api/events")
    public ResponseEntity<List<PublicEventResponse>> getPublicEvents() {
        return ResponseEntity.ok(eventService.getPublicEvents());
    }

    /**
     * GET /api/events/{link}
     * Returns the full details of a single PUBLISHED event.
     * Access: PUBLIC
     */
    @GetMapping("/api/events/{link}")
    public ResponseEntity<PublicEventResponse> getPublicEventBySlug(@PathVariable("link") String link) {
        return ResponseEntity.ok(eventService.getPublicEventBySlug(link));
    }
}

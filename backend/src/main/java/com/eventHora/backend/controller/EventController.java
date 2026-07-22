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
     * Access: ADMIN and STAFF
     */
    @GetMapping("/api/admin/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<EventSummaryResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /**
     * GET /api/admin/dashboard
     *
     * Returns a cross-event platform snapshot:
     *  - Event counts by status (published, upcoming, draft, completed, cancelled)
     *  - All-time registration and ticket counts
     *  - All-time revenue (collected, pending gate, complimentary waivers)
     *  - This-month registration and revenue stats
     *
     * Access: ADMIN and STAFF
     */
    @GetMapping("/api/admin/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(eventService.getDashboard());
    }

    /**
     * GET /api/admin/events/{id}
     * Returns the full details of a single event regardless of its status.
     * Use this before making a PATCH update call so you know the current values.
     * Access: ADMIN and STAFF
     */
    @GetMapping("/api/admin/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    /**
     * GET /api/admin/events/{eventId}/registrations
     *
     * Returns the full list of registrations for a specific event.
     * Each row includes member ID, quantity, payment status, check-in status, and booked-at timestamp.
     * Ordered by booking time (newest first).
     *
     * Access: ADMIN and STAFF
     */
    @GetMapping("/api/admin/events/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<RegistrationSummaryResponse>> getEventRegistrations(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getRegistrationsForEvent(eventId));
    }

    /**
     * GET /api/admin/events/{eventId}/payment-summary
     *
     * Returns a financial and capacity snapshot for a single event:
     *  - Seat availability (locked vs remaining)
     *  - Registration counts broken down by payment status
     *  - Gate check-in statistics (checked-in vs not-yet-arrived)
     *  - Revenue collected vs pending gate collection vs complimentary waivers
     *
     * Access: ADMIN and STAFF
     */
    @GetMapping("/api/admin/events/{eventId}/payment-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<PaymentSummaryResponse> getPaymentSummary(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getPaymentSummary(eventId));
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

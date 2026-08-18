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

    @PostMapping("/api/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        EventResponse response = eventService.createEvent(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/api/events/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request) {

        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @PatchMapping("/api/events/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> publishEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.publishEvent(id));
    }

    @DeleteMapping("/api/events/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> cancelEvent(@PathVariable UUID id) {
        eventService.cancelEvent(id);
        return ResponseEntity.ok(Map.of("message", "Event cancelled successfully"));
    }

    @GetMapping("/api/admin/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<EventSummaryResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/api/admin/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(eventService.getDashboard());
    }

    @GetMapping("/api/admin/events/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/api/admin/events/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<RegistrationSummaryResponse>> getEventRegistrations(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getRegistrationsForEvent(eventId));
    }

    @GetMapping("/api/admin/events/{eventId}/payment-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<PaymentSummaryResponse> getPaymentSummary(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getPaymentSummary(eventId));
    }

    @PostMapping(value = "/api/events/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> uploadBanner(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(eventService.uploadBanner(id, file));
    }

    @GetMapping("/api/events")
    public ResponseEntity<List<PublicEventResponse>> getPublicEvents() {
        return ResponseEntity.ok(eventService.getPublicEvents());
    }

    @GetMapping("/api/events/{link}")
    public ResponseEntity<PublicEventResponse> getPublicEventBySlug(@PathVariable("link") String link) {
        return ResponseEntity.ok(eventService.getPublicEventBySlug(link));
    }
}

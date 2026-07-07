package com.eventHora.backend.service;

import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.dto.*;
import com.eventHora.backend.exception.ResourceNotFoundException;
import com.eventHora.backend.model.Event;
import com.eventHora.backend.model.SystemUser;
import com.eventHora.backend.repository.EventRepository;
import com.eventHora.backend.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SystemUserRepository userRepository;
    private final S3Service s3Service;

    // ─── Create ───────────────────────────────────────────────────────────────

    public EventResponse createEvent(CreateEventRequest request, String adminEmail) {
        SystemUser admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        String slug = generateUniqueSlug(request.getTitle());

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .bannerUrl(request.getBannerUrl()) // null when creating; uploaded via S3 separately
                .eventDate(request.getEventDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .registrationDeadline(request.getRegistrationDeadline())
                .venue(request.getVenue())
                .additionalVenueInfo(request.getAdditionalVenueInfo())
                .totalCapacity(request.getTotalCapacity())
                .maxTicketsPerMember(request.getMaxTicketsPerMember())
                .freeTicketsPerRegistration(request.getFreeTicketsPerRegistration())
                .ticketPrice(request.getTicketPrice())
                .platformFeePerTicket(request.getPlatformFeePerTicket())
                .minimumAge(request.getMinimumAge())
                .importantNotes(request.getImportantNotes())
                .contactPersonName(request.getContactPersonName())
                .contactPersonPhone(request.getContactPersonPhone())
                .status(EventStatus.DRAFT)
                .uniqueEventLink(slug)
                .createdBy(admin)
                .build();

        return toEventResponse(eventRepository.save(event));
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public EventResponse updateEvent(UUID id, UpdateEventRequest request) {
        Event event = findEventById(id);

        // Only apply non-null fields (PATCH semantics)
        if (request.getTitle() != null)                         event.setTitle(request.getTitle());
        if (request.getDescription() != null)                   event.setDescription(request.getDescription());
        if (request.getCategory() != null)                      event.setCategory(request.getCategory());
        if (request.getBannerUrl() != null)                     event.setBannerUrl(request.getBannerUrl());
        if (request.getEventDate() != null)                     event.setEventDate(request.getEventDate());
        if (request.getStartTime() != null)                     event.setStartTime(request.getStartTime());
        if (request.getEndTime() != null)                       event.setEndTime(request.getEndTime());
        if (request.getRegistrationDeadline() != null)          event.setRegistrationDeadline(request.getRegistrationDeadline());
        if (request.getVenue() != null)                         event.setVenue(request.getVenue());
        if (request.getAdditionalVenueInfo() != null)           event.setAdditionalVenueInfo(request.getAdditionalVenueInfo());
        if (request.getTotalCapacity() != null)                 event.setTotalCapacity(request.getTotalCapacity());
        if (request.getMaxTicketsPerMember() != null)           event.setMaxTicketsPerMember(request.getMaxTicketsPerMember());
        if (request.getFreeTicketsPerRegistration() != null)    event.setFreeTicketsPerRegistration(request.getFreeTicketsPerRegistration());
        if (request.getTicketPrice() != null)                   event.setTicketPrice(request.getTicketPrice());
        if (request.getPlatformFeePerTicket() != null)          event.setPlatformFeePerTicket(request.getPlatformFeePerTicket());
        if (request.getMinimumAge() != null)                    event.setMinimumAge(request.getMinimumAge());
        if (request.getImportantNotes() != null)                event.setImportantNotes(request.getImportantNotes());
        if (request.getContactPersonName() != null)             event.setContactPersonName(request.getContactPersonName());
        if (request.getContactPersonPhone() != null)            event.setContactPersonPhone(request.getContactPersonPhone());

        return toEventResponse(eventRepository.save(event));
    }

    // ─── Publish ──────────────────────────────────────────────────────────────

    public EventResponse publishEvent(UUID id) {
        Event event = findEventById(id);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalStateException("Cannot publish a cancelled event");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return toEventResponse(eventRepository.save(event));
    }

    // ─── Cancel ───────────────────────────────────────────────────────────────

    public void cancelEvent(UUID id) {
        Event event = findEventById(id);
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed event");
        }
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);
    }

    // ─── List all (admin) ─────────────────────────────────────────────────────

    public List<EventSummaryResponse> getAllEvents() {
        return eventRepository.findAllByOrderByEventDateDesc()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    // ─── List all (public) ────────────────────────────────────────────────────

    public List<PublicEventResponse> getPublicEvents() {
        return eventRepository.findByStatusOrderByEventDateDesc(EventStatus.PUBLISHED)
                .stream()
                .map(this::toPublicEventResponse)
                .toList();
    }

    // ─── Get single event (public) ────────────────────────────────────────────

    public PublicEventResponse getPublicEventBySlug(String uniqueEventLink) {
        Event event = eventRepository.findByUniqueEventLink(uniqueEventLink)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Event not found"); // Don't expose non-published events
        }
        
        return toPublicEventResponse(event);
    }

    // ─── Banner upload ────────────────────────────────────────────────────────

    /**
     * Uploads a banner image to S3 and saves the resulting URL on the event.
     * Deletes the previous banner from S3 first if one already exists.
     */
    public EventResponse uploadBanner(UUID id, MultipartFile file) throws IOException {
        Event event = findEventById(id);

        if (event.getBannerUrl() != null && !event.getBannerUrl().isBlank()) {
            s3Service.deleteFile(event.getBannerUrl());
        }

        String bannerUrl = s3Service.uploadFile(file, "events/banners");
        event.setBannerUrl(bannerUrl);
        return toEventResponse(eventRepository.save(event));
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Event findEventById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }

    /**
     * Converts a title to a URL-safe slug.
     * "Mere Mehboob Na Ja..." → "mere-mehboob-na-ja-3f8a2b"
     * The 6-char suffix guarantees uniqueness without needing sequential numbers.
     */
    private String generateUniqueSlug(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        String slug = normalized
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String candidate = slug + "-" + suffix;

        while (eventRepository.existsByUniqueEventLink(candidate)) {
            suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
            candidate = slug + "-" + suffix;
        }
        return candidate;
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private EventResponse toEventResponse(Event event) {
        int booked = 0; // will be updated once the Registration module is in place
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .bannerUrl(event.getBannerUrl() != null && !event.getBannerUrl().isBlank() 
                        ? s3Service.generatePresignedUrl(event.getBannerUrl(), Duration.ofDays(7)) 
                        : null)
                .eventDate(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .registrationDeadline(event.getRegistrationDeadline())
                .venue(event.getVenue())
                .additionalVenueInfo(event.getAdditionalVenueInfo())
                .totalCapacity(event.getTotalCapacity())
                .bookedCount(booked)
                .availableCount(event.getTotalCapacity() - booked)
                .maxTicketsPerMember(event.getMaxTicketsPerMember())
                .freeTicketsPerRegistration(event.getFreeTicketsPerRegistration())
                .ticketPrice(event.getTicketPrice())
                .platformFeePerTicket(event.getPlatformFeePerTicket())
                .minimumAge(event.getMinimumAge())
                .importantNotes(event.getImportantNotes())
                .contactPersonName(event.getContactPersonName())
                .contactPersonPhone(event.getContactPersonPhone())
                .status(event.getStatus())
                .uniqueEventLink(event.getUniqueEventLink())
                .createdByName(event.getCreatedBy().getName())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private EventSummaryResponse toSummaryResponse(Event event) {
        int booked = 0; // TODO: Update this when Registration module is built
        boolean isSoldOut = booked >= event.getTotalCapacity();
        
        return EventSummaryResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .category(event.getCategory())
                .bannerUrl(event.getBannerUrl() != null && !event.getBannerUrl().isBlank() 
                        ? s3Service.generatePresignedUrl(event.getBannerUrl(), Duration.ofDays(7)) 
                        : null)
                .eventDate(event.getEventDate())
                .startTime(event.getStartTime())
                .venue(event.getVenue())
                .status(event.getStatus())
                .uniqueEventLink(event.getUniqueEventLink())
                .totalCapacity(event.getTotalCapacity())
                .bookedCount(booked)
                .availableCount(event.getTotalCapacity() - booked)
                .registrationOpen(
                        event.getStatus() == EventStatus.PUBLISHED
                        && event.getRegistrationDeadline().isAfter(java.time.LocalDateTime.now())
                        && !isSoldOut
                )
                .isSoldOut(isSoldOut)
                .build();
    }

    private PublicEventResponse toPublicEventResponse(Event event) {
        int booked = 0; // TODO: Update this when Registration module is built
        boolean isSoldOut = booked >= event.getTotalCapacity();
        
        return PublicEventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .bannerUrl(event.getBannerUrl() != null && !event.getBannerUrl().isBlank() 
                        ? s3Service.generatePresignedUrl(event.getBannerUrl(), Duration.ofDays(7)) 
                        : null)
                .eventDate(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .registrationDeadline(event.getRegistrationDeadline())
                .venue(event.getVenue())
                .additionalVenueInfo(event.getAdditionalVenueInfo())
                .maxTicketsPerMember(event.getMaxTicketsPerMember())
                .freeTicketsPerRegistration(event.getFreeTicketsPerRegistration())
                .ticketPrice(event.getTicketPrice())
                .minimumAge(event.getMinimumAge())
                .importantNotes(event.getImportantNotes())
                .contactPersonName(event.getContactPersonName())
                .contactPersonPhone(event.getContactPersonPhone())
                .uniqueEventLink(event.getUniqueEventLink())
                .registrationOpen(
                        event.getStatus() == EventStatus.PUBLISHED
                        && event.getRegistrationDeadline().isAfter(java.time.LocalDateTime.now())
                        && !isSoldOut
                )
                .isSoldOut(isSoldOut)
                .build();
    }
}

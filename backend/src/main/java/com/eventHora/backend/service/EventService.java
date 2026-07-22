package com.eventHora.backend.service;

import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.Enum.PaymentStatus;
import com.eventHora.backend.dto.*;
import com.eventHora.backend.exception.ResourceNotFoundException;
import com.eventHora.backend.model.Event;
import com.eventHora.backend.model.Registration;
import com.eventHora.backend.model.SystemUser;
import com.eventHora.backend.repository.EventRepository;
import com.eventHora.backend.repository.RegistrationRepository;
import com.eventHora.backend.repository.SystemUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SystemUserRepository userRepository;
    private final S3Service s3Service;
    private final RegistrationRepository registrationRepository;

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

    // ─── Get single event (admin) ─────────────────────────────────────────────

    /**
     * Returns the full EventResponse for any event regardless of status.
     * Used by the admin dashboard before making a PATCH call.
     */
    public EventResponse getEventById(UUID id) {
        return toEventResponse(findEventById(id));
    }

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
        int booked = registrationRepository.sumLockedTicketsForEvent(event.getId());
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
        int booked = registrationRepository.sumLockedTicketsForEvent(event.getId());
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
        int booked = registrationRepository.sumLockedTicketsForEvent(event.getId());
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
    // ─── Phase 7A: Admin Registration List ───────────────────────────────────────

    /**
     * GET /api/admin/events/{eventId}/registrations
     *
     * Returns all registrations for a given event, ordered by booking time (newest first).
     * Verifies the event exists before querying registrations so we return a meaningful 404
     * rather than an empty list when the event ID is wrong.
     *
     * The LAZY association to Event on Registration is resolved inside this @Transactional
     * method, so event.getTitle() etc. are safe to call without LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public List<RegistrationSummaryResponse> getRegistrationsForEvent(UUID eventId) {

        // Verify event exists — throw 404 if not
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        List<Registration> registrations = registrationRepository
                .findByEventIdOrderByBookedAtDesc(eventId);

        log.info("Admin: fetched {} registrations for event '{}'",
                registrations.size(), event.getTitle());

        return registrations.stream()
                .map(this::toRegistrationSummaryResponse)
                .toList();
    }

    private RegistrationSummaryResponse toRegistrationSummaryResponse(Registration r) {
        return RegistrationSummaryResponse.builder()
                .registrationId(r.getId())
                .ticketReference(r.getTicketReference())
                .memberId(r.getMemberId())
                .memberType(r.getMemberType())
                .quantity(r.getQuantity())
                .totalAmount(r.getTotalAmount())
                .paymentStatus(r.getPaymentStatus())
                .paymentPreference(r.getPaymentPreference())
                .isCheckedIn(r.isCheckedIn())
                .checkedInAt(r.getCheckedInAt())
                .bookedAt(r.getBookedAt())
                .build();
    }

    // ─── Phase 7B: Admin Payment Summary ─────────────────────────────────────────

    /**
     * GET /api/admin/events/{eventId}/payment-summary
     *
     * Computes a full payment + capacity snapshot for one event.
     * Uses a single aggregation query (GROUP BY payment_status) to minimise DB round-trips.
     *
     * All amounts are BigDecimal to preserve financial precision.
     */
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary(UUID eventId) {

        // Verify event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        // One DB call: aggregates per status
        List<Object[]> aggregates = registrationRepository.getPaymentAggregatesByEventId(eventId);

        // Initialise counters
        long confirmedCount      = 0;
        long payAtGateCount      = 0;
        long freeCount           = 0;
        long complimentaryCount  = 0;
        long pendingCount        = 0;
        long failedCount         = 0;
        long totalRegistrations  = 0;

        BigDecimal totalRevenue          = BigDecimal.ZERO;
        BigDecimal pendingGateCollection = BigDecimal.ZERO;
        BigDecimal complimentaryWaived   = BigDecimal.ZERO;

        // Walk the aggregation rows
        for (Object[] row : aggregates) {
            String status         = (String) row[0];
            long   regCount       = ((Number) row[1]).longValue();
            BigDecimal amount     = new BigDecimal(row[3].toString());

            totalRegistrations += regCount;

            PaymentStatus ps = PaymentStatus.valueOf(status);
            switch (ps) {
                case CONFIRMED     -> { confirmedCount     = regCount; totalRevenue          = totalRevenue.add(amount); }
                case PAY_AT_GATE   -> { payAtGateCount     = regCount; pendingGateCollection = pendingGateCollection.add(amount); }
                case FREE          -> { freeCount          = regCount; }
                case COMPLIMENTARY -> { complimentaryCount = regCount; complimentaryWaived   = complimentaryWaived.add(amount); }
                case PENDING       -> { pendingCount       = regCount; }
                case FAILED        -> { failedCount        = regCount; }
            }
        }

        // Locked seats = CONFIRMED + FREE + PAY_AT_GATE + COMPLIMENTARY (in TICKETS)
        int seatsLocked    = registrationRepository.sumLockedTicketsForEvent(eventId);
        int seatsRemaining = Math.max(0, event.getTotalCapacity() - seatsLocked);

        // Check-in stats — BOOKING level (how many members/registrations have arrived)
        long checkedInCount    = registrationRepository.countCheckedInForEvent(eventId);
        // Locked bookings = confirmedCount + payAtGateCount + freeCount + complimentaryCount
        long lockedBookings    = confirmedCount + payAtGateCount + freeCount + complimentaryCount;
        long notCheckedInCount = Math.max(0, lockedBookings - checkedInCount);

        // Check-in stats — TICKET level (comparable to seatsLocked, which is also in tickets)
        long checkedInTickets    = registrationRepository.sumCheckedInTicketsForEvent(eventId);
        long notCheckedInTickets = Math.max(0, seatsLocked - checkedInTickets);

        log.info("Admin payment summary for event '{}': {} locked seats, {} checked-in bookings, {} checked-in tickets, revenue={}",
                event.getTitle(), seatsLocked, checkedInCount, checkedInTickets, totalRevenue);

        return PaymentSummaryResponse.builder()
                .totalCapacity(event.getTotalCapacity())
                .seatsLocked(seatsLocked)
                .seatsRemaining(seatsRemaining)
                .confirmedCount(confirmedCount)
                .payAtGateCount(payAtGateCount)
                .freeCount(freeCount)
                .complimentaryCount(complimentaryCount)
                .pendingCount(pendingCount)
                .failedCount(failedCount)
                .totalRegistrations(totalRegistrations)
                .checkedInCount(checkedInCount)
                .notCheckedInCount(notCheckedInCount)
                .checkedInTickets(checkedInTickets)
                .notCheckedInTickets(notCheckedInTickets)
                .totalRevenue(totalRevenue)
                .pendingGateCollection(pendingGateCollection)
                .complimentaryWaived(complimentaryWaived)
                .build();
    }
}

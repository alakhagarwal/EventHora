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

    public EventResponse createEvent(CreateEventRequest request, String adminEmail) {
        SystemUser admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        String slug = generateUniqueSlug(request.getTitle());

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .bannerUrl(request.getBannerUrl())
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

    public EventResponse updateEvent(UUID id, UpdateEventRequest request) {
        Event event = findEventById(id);

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

    public EventResponse publishEvent(UUID id) {
        Event event = findEventById(id);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalStateException("Cannot publish a cancelled event");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return toEventResponse(eventRepository.save(event));
    }

    public void cancelEvent(UUID id) {
        Event event = findEventById(id);
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed event");
        }
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);
    }

    public List<EventSummaryResponse> getAllEvents() {
        return eventRepository.findAllByOrderByEventDateDesc()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public List<PublicEventResponse> getPublicEvents() {
        return eventRepository.findByStatusOrderByEventDateDesc(EventStatus.PUBLISHED)
                .stream()
                .map(this::toPublicEventResponse)
                .toList();
    }

    public EventResponse getEventById(UUID id) {
        return toEventResponse(findEventById(id));
    }

    public PublicEventResponse getPublicEventBySlug(String uniqueEventLink) {
        Event event = eventRepository.findByUniqueEventLink(uniqueEventLink)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Event not found");
        }

        return toPublicEventResponse(event);
    }

    public EventResponse uploadBanner(UUID id, MultipartFile file) throws IOException {
        Event event = findEventById(id);

        if (event.getBannerUrl() != null && !event.getBannerUrl().isBlank()) {
            s3Service.deleteFile(event.getBannerUrl());
        }

        String bannerUrl = s3Service.uploadFile(file, "events/banners");
        event.setBannerUrl(bannerUrl);
        return toEventResponse(eventRepository.save(event));
    }

    private Event findEventById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }

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
        int available = Math.max(0, event.getTotalCapacity() - booked);
        boolean isSoldOut = available == 0;

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
                .totalCapacity(event.getTotalCapacity())
                .availableCount(available)
                .uniqueEventLink(event.getUniqueEventLink())
                .registrationOpen(
                        event.getStatus() == EventStatus.PUBLISHED
                        && event.getRegistrationDeadline().isAfter(java.time.LocalDateTime.now())
                        && !isSoldOut
                )
                .isSoldOut(isSoldOut)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RegistrationSummaryResponse> getRegistrationsForEvent(UUID eventId) {

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

    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary(UUID eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        List<Object[]> aggregates = registrationRepository.getPaymentAggregatesByEventId(eventId);

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

        int seatsLocked    = registrationRepository.sumLockedTicketsForEvent(eventId);
        int seatsRemaining = Math.max(0, event.getTotalCapacity() - seatsLocked);

        long checkedInCount    = registrationRepository.countCheckedInForEvent(eventId);

        long lockedBookings    = confirmedCount + payAtGateCount + freeCount + complimentaryCount;
        long notCheckedInCount = Math.max(0, lockedBookings - checkedInCount);

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

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {

        long totalEvents      = 0;
        long publishedEvents  = 0;
        long draftEvents      = 0;
        long completedEvents  = 0;
        long cancelledEvents  = 0;

        for (Object[] row : eventRepository.countEventsByStatus()) {
            String status = (String) row[0];
            long   count  = ((Number) row[1]).longValue();
            totalEvents  += count;
            switch (EventStatus.valueOf(status)) {
                case PUBLISHED  -> publishedEvents  = count;
                case DRAFT      -> draftEvents      = count;
                case COMPLETED  -> completedEvents  = count;
                case CANCELLED  -> cancelledEvents  = count;
            }
        }

        long upcomingEvents = eventRepository.countUpcomingEvents(java.time.LocalDate.now());

        long totalRegistrations  = 0;
        long lockedRegistrations = 0;
        long totalTicketsSold    = 0;

        BigDecimal totalRevenue          = BigDecimal.ZERO;
        BigDecimal pendingGateCollection = BigDecimal.ZERO;
        BigDecimal complimentaryWaived   = BigDecimal.ZERO;

        for (Object[] row : registrationRepository.getGlobalPaymentAggregates()) {
            String     status    = (String) row[0];
            long       regCount  = ((Number) row[1]).longValue();
            long       ticketCnt = ((Number) row[2]).longValue();
            BigDecimal amount    = new BigDecimal(row[3].toString());

            totalRegistrations += regCount;

            PaymentStatus ps = PaymentStatus.valueOf(status);
            switch (ps) {
                case CONFIRMED -> {
                    lockedRegistrations += regCount;
                    totalTicketsSold    += ticketCnt;
                    totalRevenue         = totalRevenue.add(amount);
                }
                case FREE -> {
                    lockedRegistrations += regCount;
                    totalTicketsSold    += ticketCnt;
                }
                case PAY_AT_GATE -> {
                    lockedRegistrations  += regCount;
                    totalTicketsSold     += ticketCnt;
                    pendingGateCollection = pendingGateCollection.add(amount);
                }
                case COMPLIMENTARY -> {
                    lockedRegistrations += regCount;
                    totalTicketsSold    += ticketCnt;
                    complimentaryWaived  = complimentaryWaived.add(amount);
                }
                case PENDING, FAILED -> {  }
            }
        }

        java.time.LocalDateTime startOfMonth = java.time.LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay();

        long registrationsThisMonth = 0;
        long ticketsSoldThisMonth   = 0;
        BigDecimal revenueThisMonth  = BigDecimal.ZERO;

        for (Object[] row : registrationRepository.getMonthlyPaymentAggregates(startOfMonth)) {
            String     status    = (String) row[0];
            long       regCount  = ((Number) row[1]).longValue();
            long       ticketCnt = ((Number) row[2]).longValue();
            BigDecimal amount    = new BigDecimal(row[3].toString());

            PaymentStatus ps = PaymentStatus.valueOf(status);

            registrationsThisMonth += regCount;

            switch (ps) {
                case CONFIRMED, FREE, PAY_AT_GATE, COMPLIMENTARY -> ticketsSoldThisMonth += ticketCnt;
                default -> {  }
            }

            if (ps == PaymentStatus.CONFIRMED) {
                revenueThisMonth = revenueThisMonth.add(amount);
            }
        }

        log.info("Dashboard: {} events ({} upcoming), {} locked registrations, all-time revenue={}",
                totalEvents, upcomingEvents, lockedRegistrations, totalRevenue);

        return DashboardResponse.builder()
                .totalEvents(totalEvents)
                .publishedEvents(publishedEvents)
                .upcomingEvents(upcomingEvents)
                .draftEvents(draftEvents)
                .completedEvents(completedEvents)
                .cancelledEvents(cancelledEvents)
                .totalRegistrations(totalRegistrations)
                .lockedRegistrations(lockedRegistrations)
                .totalTicketsSold(totalTicketsSold)
                .registrationsThisMonth(registrationsThisMonth)
                .ticketsSoldThisMonth(ticketsSoldThisMonth)
                .totalRevenue(totalRevenue)
                .pendingGateCollection(pendingGateCollection)
                .complimentaryWaived(complimentaryWaived)
                .revenueThisMonth(revenueThisMonth)
                .build();
    }
}

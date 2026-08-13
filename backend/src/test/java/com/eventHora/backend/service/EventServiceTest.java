//package com.eventHora.backend.service;
//
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//import static org.mockito.ArgumentMatchers.*;
//
//import com.eventHora.backend.Enum.EventCategory;
//import com.eventHora.backend.Enum.EventStatus;
//import com.eventHora.backend.dto.CreateEventRequest;
//import com.eventHora.backend.dto.EventResponse;
//import com.eventHora.backend.exception.ResourceNotFoundException;
//import com.eventHora.backend.model.Event;
//import com.eventHora.backend.model.SystemUser;
//import com.eventHora.backend.repository.EventRepository;
//import com.eventHora.backend.repository.RegistrationRepository;
//import com.eventHora.backend.repository.SystemUserRepository;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("EventService Tests")
//class EventServiceTest {
//
//    @Mock
//    private EventRepository eventRepository;
//
//    @Mock
//    private SystemUserRepository userRepository;
//
//    @Mock
//    private S3Service s3Service;
//
//    @Mock
//    private RegistrationRepository registrationRepository;
//
//    @InjectMocks
//    private EventService eventService;
//
//    @Captor
//    private ArgumentCaptor<Event> eventCaptor;
//
//    private SystemUser fakeAdmin;
//    private CreateEventRequest validRequest;
//
//    @BeforeEach
//    void setUp() {
//        fakeAdmin = SystemUser.builder()
//                .id(UUID.randomUUID())
//                .name("Test Admin")
//                .email("admin@test.com")
//                .build();
//
//        validRequest = new CreateEventRequest();
//        validRequest.setTitle("Tech Conference 2026");
//        validRequest.setDescription("A great event about Java and Spring Boot");
//        validRequest.setCategory(EventCategory.CULTURAL);
//        validRequest.setEventDate(LocalDate.now().plusDays(30));
//        validRequest.setStartTime(LocalTime.of(18, 0));
//        validRequest.setEndTime(LocalTime.of(21, 0));
//        validRequest.setRegistrationDeadline(LocalDateTime.now().plusDays(25));
//        validRequest.setVenue("Main Auditorium, RIC");
//        validRequest.setTotalCapacity(200);
//        validRequest.setMaxTicketsPerMember(4);
//        validRequest.setFreeTicketsPerRegistration(2);
//        validRequest.setTicketPrice(BigDecimal.valueOf(500));
//        validRequest.setPlatformFeePerTicket(BigDecimal.valueOf(20));
//    }
//
//    @Nested
//    @DisplayName("createEvent()")
//    class CreateEventTests {
//
//        @Test
//        @DisplayName("Should create and save event when admin exists")
//        void shouldCreateEventAndSaveItWhenAdminExists() {
//            when(userRepository.findByEmail("admin@test.com"))
//                    .thenReturn(Optional.of(fakeAdmin));
//            when(eventRepository.existsByUniqueEventLink(anyString()))
//                    .thenReturn(false);
//
//            Event savedEvent = buildFakeEvent(UUID.randomUUID(), EventStatus.DRAFT);
//            when(eventRepository.save(any(Event.class)))
//                    .thenReturn(savedEvent);
//            when(registrationRepository.sumLockedTicketsForEvent(any(UUID.class)))
//                    .thenReturn(0);
//
//            EventResponse response = eventService.createEvent(validRequest, "admin@test.com");
//
//            assertNotNull(response);
//            assertEquals(EventStatus.DRAFT, response.getStatus());
//            assertEquals("Tech Conference 2026", response.getTitle());
//
//            verify(userRepository, times(1)).findByEmail("admin@test.com");
//            verify(eventRepository, times(1)).save(any(Event.class));
//        }
//
//        @Test
//        @DisplayName("Created event should have DRAFT status by default")
//        void createdEventShouldBeInDraftStatus() {
//            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(fakeAdmin));
//            when(eventRepository.existsByUniqueEventLink(anyString())).thenReturn(false);
//
//            Event savedEvent = buildFakeEvent(UUID.randomUUID(), EventStatus.DRAFT);
//            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
//            when(registrationRepository.sumLockedTicketsForEvent(any())).thenReturn(0);
//
//            EventResponse response = eventService.createEvent(validRequest, "admin@test.com");
//
//            assertEquals(EventStatus.DRAFT, response.getStatus());
//        }
//
//        @Test
//        @DisplayName("Should throw ResourceNotFoundException when admin email doesn't exist")
//        void shouldThrowWhenAdminNotFound() {
//            when(userRepository.findByEmail("nobody@test.com"))
//                    .thenReturn(Optional.empty());
//
//            ResourceNotFoundException ex = assertThrows(
//                    ResourceNotFoundException.class,
//                    () -> eventService.createEvent(validRequest, "nobody@test.com")
//            );
//
//            assertTrue(ex.getMessage().contains("Admin not found"));
//            verify(eventRepository, never()).save(any());
//        }
//
//        @Test
//        @DisplayName("Should pass the correct Event object to save()")
//        void shouldPassCorrectEventDataToRepository() {
//            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(fakeAdmin));
//            when(eventRepository.existsByUniqueEventLink(anyString())).thenReturn(false);
//            when(eventRepository.save(eventCaptor.capture()))
//                    .thenAnswer(invocation -> {
//                        Event e = invocation.getArgument(0);
//                        return buildFakeEvent(UUID.randomUUID(), e.getStatus());
//                    });
//            when(registrationRepository.sumLockedTicketsForEvent(any())).thenReturn(0);
//
//            eventService.createEvent(validRequest, "admin@test.com");
//
//            Event capturedEvent = eventCaptor.getValue();
//            assertNotNull(capturedEvent);
//            assertEquals("Tech Conference 2026", capturedEvent.getTitle());
//            assertEquals(EventStatus.DRAFT, capturedEvent.getStatus());
//            assertEquals(fakeAdmin, capturedEvent.getCreatedBy());
//            assertEquals(200, capturedEvent.getTotalCapacity());
//        }
//    }
//
//    @Nested
//    @DisplayName("publishEvent()")
//    class PublishEventTests {
//
//        @Test
//        @DisplayName("Should publish a DRAFT event successfully")
//        void shouldPublishDraftEvent() {
//            UUID eventId = UUID.randomUUID();
//            Event draftEvent = buildFakeEvent(eventId, EventStatus.DRAFT);
//
//            when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
//            when(eventRepository.save(any(Event.class))).thenReturn(draftEvent);
//            when(registrationRepository.sumLockedTicketsForEvent(any())).thenReturn(0);
//
//            EventResponse response = eventService.publishEvent(eventId);
//
//            assertEquals(EventStatus.PUBLISHED, response.getStatus());
//            verify(eventRepository, times(1)).save(draftEvent);
//        }
//
//        @Test
//        @DisplayName("Should throw IllegalStateException when publishing a CANCELLED event")
//        void shouldThrowWhenPublishingCancelledEvent() {
//            UUID eventId = UUID.randomUUID();
//            Event cancelledEvent = buildFakeEvent(eventId, EventStatus.CANCELLED);
//
//            when(eventRepository.findById(eventId)).thenReturn(Optional.of(cancelledEvent));
//
//            IllegalStateException ex = assertThrows(
//                    IllegalStateException.class,
//                    () -> eventService.publishEvent(eventId)
//            );
//
//            assertEquals("Cannot publish a cancelled event", ex.getMessage());
//            verify(eventRepository, never()).save(any());
//        }
//
//        @Test
//        @DisplayName("Should throw ResourceNotFoundException for a non-existent event ID")
//        void shouldThrowWhenEventIdNotFound() {
//            UUID missingId = UUID.randomUUID();
//            when(eventRepository.findById(missingId)).thenReturn(Optional.empty());
//
//            assertThrows(
//                    ResourceNotFoundException.class,
//                    () -> eventService.publishEvent(missingId)
//            );
//        }
//    }
//
//    @Nested
//    @DisplayName("cancelEvent()")
//    class CancelEventTests {
//
//        @Test
//        @DisplayName("Should cancel a PUBLISHED event")
//        void shouldCancelPublishedEvent() {
//            UUID eventId = UUID.randomUUID();
//            Event publishedEvent = buildFakeEvent(eventId, EventStatus.PUBLISHED);
//
//            when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
//            when(eventRepository.save(any(Event.class))).thenReturn(publishedEvent);
//
//            eventService.cancelEvent(eventId);
//
//            verify(eventRepository).save(eventCaptor.capture());
//            assertEquals(EventStatus.CANCELLED, eventCaptor.getValue().getStatus());
//        }
//
//        @Test
//        @DisplayName("Should throw IllegalStateException when cancelling a COMPLETED event")
//        void shouldThrowWhenCancellingCompletedEvent() {
//            UUID eventId = UUID.randomUUID();
//            Event completedEvent = buildFakeEvent(eventId, EventStatus.COMPLETED);
//
//            when(eventRepository.findById(eventId)).thenReturn(Optional.of(completedEvent));
//
//            IllegalStateException ex = assertThrows(
//                    IllegalStateException.class,
//                    () -> eventService.cancelEvent(eventId)
//            );
//
//            assertEquals("Cannot cancel a completed event", ex.getMessage());
//            verify(eventRepository, never()).save(any());
//        }
//    }
//
//    @Nested
//    @DisplayName("getAllEvents()")
//    class GetAllEventsTests {
//
//        @Test
//        @DisplayName("Should return all events as a mapped list")
//        void shouldReturnAllEventsAsList() {
//            List<Event> fakeEvents = List.of(
//                    buildFakeEvent(UUID.randomUUID(), EventStatus.PUBLISHED),
//                    buildFakeEvent(UUID.randomUUID(), EventStatus.DRAFT),
//                    buildFakeEvent(UUID.randomUUID(), EventStatus.CANCELLED)
//            );
//            when(eventRepository.findAllByOrderByEventDateDesc()).thenReturn(fakeEvents);
//            when(registrationRepository.sumLockedTicketsForEvent(any())).thenReturn(0);
//
//            var responses = eventService.getAllEvents();
//
//            assertNotNull(responses);
//            assertEquals(3, responses.size());
//            verify(eventRepository, times(1)).findAllByOrderByEventDateDesc();
//        }
//
//        @Test
//        @DisplayName("Should return empty list when there are no events")
//        void shouldReturnEmptyListWhenNoEventsExist() {
//            when(eventRepository.findAllByOrderByEventDateDesc()).thenReturn(List.of());
//
//            var responses = eventService.getAllEvents();
//
//            assertNotNull(responses);
//            assertTrue(responses.isEmpty());
//        }
//    }
//
//    @Nested
//    @DisplayName("getEventById()")
//    class GetEventByIdTests {
//
//        @Test
//        @DisplayName("Should return event response when event exists")
//        void shouldReturnEventWhenFound() {
//            UUID eventId = UUID.randomUUID();
//            Event event = buildFakeEvent(eventId, EventStatus.PUBLISHED);
//
//            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
//            when(registrationRepository.sumLockedTicketsForEvent(eventId)).thenReturn(50);
//
//            EventResponse response = eventService.getEventById(eventId);
//
//            assertNotNull(response);
//            assertEquals(eventId, response.getId());
//            assertEquals(50, response.getBookedCount());
//            assertEquals(150, response.getAvailableCount());
//        }
//
//        @Test
//        @DisplayName("Should throw when event ID doesn't exist")
//        void shouldThrowWhenEventNotFound() {
//            UUID missingId = UUID.randomUUID();
//            when(eventRepository.findById(missingId)).thenReturn(Optional.empty());
//
//            assertThrows(
//                    ResourceNotFoundException.class,
//                    () -> eventService.getEventById(missingId)
//            );
//        }
//    }
//
//    @Nested
//    @DisplayName("ArgumentMatchers Showcase")
//    class ArgumentMatchersDemo {
//
//        @Test
//        @DisplayName("Demonstrating argument matchers")
//        void demonstratingArgumentMatchers() {
//            UUID specificId = UUID.fromString("11111111-1111-1111-1111-111111111111");
//            Event event = buildFakeEvent(specificId, EventStatus.DRAFT);
//
//            when(eventRepository.findById(eq(specificId)))
//                    .thenReturn(Optional.of(event));
//
//            Optional<Event> result = eventRepository.findById(specificId);
//            assertTrue(result.isPresent());
//
//            verify(eventRepository).findById(eq(specificId));
//        }
//    }
//
//    private Event buildFakeEvent(UUID id, EventStatus status) {
//        return Event.builder()
//                .id(id)
//                .title("Tech Conference 2026")
//                .description("A great event about Java and Spring Boot")
//                .category(EventCategory.CULTURAL)
//                .eventDate(LocalDate.now().plusDays(30))
//                .startTime(LocalTime.of(18, 0))
//                .endTime(LocalTime.of(21, 0))
//                .registrationDeadline(LocalDateTime.now().plusDays(25))
//                .venue("Main Auditorium, RIC")
//                .totalCapacity(200)
//                .maxTicketsPerMember(4)
//                .freeTicketsPerRegistration(2)
//                .ticketPrice(BigDecimal.valueOf(500))
//                .platformFeePerTicket(BigDecimal.valueOf(20))
//                .status(status)
//                .uniqueEventLink("tech-conference-2026-abc123")
//                .createdBy(fakeAdmin)
//                .build();
//    }
//}
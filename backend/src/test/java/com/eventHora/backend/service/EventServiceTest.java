package com.eventHora.backend.service;

// ══════════════════════════════════════════════════════════════════════════════
//  EventServiceTest.java  —  A complete JUnit 5 + Mockito learning example
//
//  CONCEPTS COVERED (in order of appearance):
//  ① @ExtendWith(MockitoExtension.class)  — JUnit 5 extension that activates Mockito
//  ② @Mock                                — creates a fake (mock) dependency
//  ③ @InjectMocks                         — creates the real class under test and injects all @Mocks
//  ④ @BeforeEach                          — runs before EVERY test method
//  ⑤ @Test                               — marks a method as a test case
//  ⑥ when(...).thenReturn(...)            — stubbing: define what a mock returns
//  ⑦ when(...).thenThrow(...)             — stubbing: make a mock throw an exception
//  ⑧ assertX(...)                         — assertions: verify the result is what you expect
//  ⑨ verify(...)                          — verify that a mock method was actually called
//  ⑩ @Captor / ArgumentCaptor             — capture the argument passed to a mock
//  ⑪ assertThrows(...)                    — assert that an exception is thrown
//  ⑫ @Nested                              — groups related tests together for readability
//  ⑬ @DisplayName                         — human-readable test names
// ══════════════════════════════════════════════════════════════════════════════

// ─── JUnit 5 imports ──────────────────────────────────────────────────────────
import org.junit.jupiter.api.*;                     // @Test, @BeforeEach, @Nested, @DisplayName, etc.
import org.junit.jupiter.api.extension.ExtendWith;  // @ExtendWith — connects JUnit 5 with Mockito

// ─── Mockito imports ──────────────────────────────────────────────────────────
import org.mockito.*;                               // @Mock, @InjectMocks, @Captor, ArgumentCaptor
import org.mockito.junit.jupiter.MockitoExtension; // The Mockito JUnit 5 Extension

// ─── JUnit 5 Assertion imports ────────────────────────────────────────────────
import static org.junit.jupiter.api.Assertions.*;  // assertEquals, assertThrows, assertNotNull, etc.

// ─── Mockito static method imports ────────────────────────────────────────────
import static org.mockito.Mockito.*;               // when(), verify(), times(), never(), any(), etc.
import static org.mockito.ArgumentMatchers.*;      // any(), eq(), anyString(), etc.

// ─── Your project imports ─────────────────────────────────────────────────────
import com.eventHora.backend.Enum.EventCategory;
import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.dto.CreateEventRequest;
import com.eventHora.backend.dto.EventResponse;
import com.eventHora.backend.exception.ResourceNotFoundException;
import com.eventHora.backend.model.Event;
import com.eventHora.backend.model.SystemUser;
import com.eventHora.backend.repository.EventRepository;
import com.eventHora.backend.repository.RegistrationRepository;
import com.eventHora.backend.repository.SystemUserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ══════════════════════════════════════════════════════════════════════════════
//  ① @ExtendWith(MockitoExtension.class)
//
//  This line tells JUnit 5: "Hey, before you run any test in this class,
//  activate the Mockito Extension first."
//
//  What does the extension do?
//  • It scans the class for @Mock, @InjectMocks, @Captor annotations.
//  • It initialises all mocks automatically (you don't need MockitoAnnotations.openMocks(this)).
//  • It validates that you are not misusing Mockito APIs (strict stubbing by default).
//  • It automatically resets mocks after each test so tests are independent.
//
//  Without this: your @Mock fields will be null at runtime and tests will crash.
// ══════════════════════════════════════════════════════════════════════════════
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Tests — JUnit 5 + Mockito Learning Guide")
class EventServiceTest {

    // ══════════════════════════════════════════════════════════════════════════
    //  ② @Mock
    //
    //  A "mock" is a fake, lightweight object that looks like the real class
    //  but does NOTHING by default — all methods return null / 0 / false.
    //
    //  WHY do we mock dependencies?
    //  • EventService depends on EventRepository, SystemUserRepository, etc.
    //  • Those repositories need a running database to work.
    //  • We DON'T want a database in unit tests — they must be fast and isolated.
    //  • So we replace the real repository with a fake one we fully control.
    //
    //  The @Mock annotation creates this fake object for us automatically.
    // ══════════════════════════════════════════════════════════════════════════
    @Mock
    private EventRepository eventRepository;

    @Mock
    private SystemUserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private RegistrationRepository registrationRepository;

    // ══════════════════════════════════════════════════════════════════════════
    //  ③ @InjectMocks
    //
    //  This creates a REAL instance of EventService (not a mock!), and then
    //  automatically injects all the @Mock fields above into it.
    //
    //  Think of it like this:
    //    new EventService(eventRepository, userRepository, s3Service, registrationRepository)
    //  ... but Mockito does it for you, matching by type.
    //
    //  This is the class we are ACTUALLY testing — all others are mocked.
    // ══════════════════════════════════════════════════════════════════════════
    @InjectMocks
    private EventService eventService;

    // ══════════════════════════════════════════════════════════════════════════
    //  ⑩ @Captor / ArgumentCaptor
    //
    //  When you call eventRepository.save(event), you often want to inspect
    //  the exact Event object that was passed. ArgumentCaptor "captures" that
    //  argument so you can assert on its fields.
    //
    //  @Captor is just a shorthand — Mockito creates the captor for you.
    // ══════════════════════════════════════════════════════════════════════════
    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    // ─── Shared test data (helpers) ───────────────────────────────────────────

    private SystemUser fakeAdmin;
    private CreateEventRequest validRequest;

    // ══════════════════════════════════════════════════════════════════════════
    //  ④ @BeforeEach
    //
    //  This method runs BEFORE EVERY single @Test method in this class.
    //  It's the perfect place to set up objects you need in multiple tests.
    //
    //  Think of it as "reset the table before every meal".
    //  Each test starts fresh — no shared state bleeds between tests.
    //
    //  JUnit 5 also has:
    //  • @AfterEach  — runs after every test (good for cleanup)
    //  • @BeforeAll  — runs once before all tests in the class (must be static)
    //  • @AfterAll   — runs once after all tests (must be static)
    // ══════════════════════════════════════════════════════════════════════════
    @BeforeEach
    void setUp() {
        // Build a fake admin user we'll reuse across tests
        fakeAdmin = SystemUser.builder()
                .id(UUID.randomUUID())
                .name("Test Admin")
                .email("admin@test.com")
                .build();

        // Build a valid CreateEventRequest we'll reuse
        validRequest = new CreateEventRequest();
        validRequest.setTitle("Tech Conference 2026");
        validRequest.setDescription("A great event about Java and Spring Boot");
        validRequest.setCategory(EventCategory.CULTURAL);
        validRequest.setEventDate(LocalDate.now().plusDays(30));
        validRequest.setStartTime(LocalTime.of(18, 0));
        validRequest.setEndTime(LocalTime.of(21, 0));
        validRequest.setRegistrationDeadline(LocalDateTime.now().plusDays(25));
        validRequest.setVenue("Main Auditorium, RIC");
        validRequest.setTotalCapacity(200);
        validRequest.setMaxTicketsPerMember(4);
        validRequest.setFreeTicketsPerRegistration(2);
        validRequest.setTicketPrice(BigDecimal.valueOf(500));
        validRequest.setPlatformFeePerTicket(BigDecimal.valueOf(20));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ⑫ @Nested
    //
    //  @Nested lets you group related test methods inside an inner class.
    //  This makes your test file readable — like a chapter book.
    //
    //  Output in the test runner:
    //   EventService Tests
    //     ✔ createEvent()
    //       ✔ should create event and save it when admin exists
    //       ✔ should throw exception when admin not found
    //       ...
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createEvent()")
    class CreateEventTests {

        // ══════════════════════════════════════════════════════════════════════
        //  ⑤ @Test
        //
        //  The most fundamental JUnit annotation. It marks a method as a test.
        //  JUnit will find it and run it. If the method throws any exception,
        //  the test FAILS. If it runs to completion without error, it PASSES.
        //
        //  ⑬ @DisplayName
        //  Gives a human-readable name shown in the test runner / IDE output.
        //  Use plain English — describe the BEHAVIOUR, not the method name.
        // ══════════════════════════════════════════════════════════════════════
        @Test
        @DisplayName("✅ Happy Path: should create and save event when admin exists")
        void shouldCreateEventAndSaveItWhenAdminExists() {

            // ──────────────────────────────────────────────────────────────────
            //  ⑥ when(...).thenReturn(...)  —  STUBBING
            //
            //  "Stubbing" means: telling the mock WHAT TO RETURN when called.
            //
            //  The real EventService.createEvent() does:
            //    userRepository.findByEmail(adminEmail)  → needs a SystemUser
            //    eventRepository.existsByUniqueEventLink(slug) → needs false (no conflict)
            //    eventRepository.save(event) → needs to return a saved event
            //    registrationRepository.sumLockedTicketsForEvent(id) → for the response
            //
            //  We define all of those returns here — WITHOUT a real database!
            //
            //  SYNTAX:
            //    when( mockObject.method(argument) ).thenReturn( valueToReturn );
            //
            //  ArgumentMatchers:
            //    • anyString()  — match any String argument
            //    • any(X.class) — match any object of type X
            //    • eq("hello")  — match exactly "hello"
            //    Without a matcher, Mockito matches by equality (.equals())
            // ──────────────────────────────────────────────────────────────────

            // 1. Stub: when the service looks up the admin by email, return our fakeAdmin
            when(userRepository.findByEmail("admin@test.com"))
                    .thenReturn(Optional.of(fakeAdmin));

            // 2. Stub: when the service checks if a slug already exists, say NO (no conflict)
            when(eventRepository.existsByUniqueEventLink(anyString()))
                    .thenReturn(false);

            // 3. Stub: when the service saves the event, return a fake saved event back
            //    We use any(Event.class) because the exact slug is randomly generated
            Event savedEvent = buildFakeEvent(UUID.randomUUID(), EventStatus.DRAFT);
            when(eventRepository.save(any(Event.class)))
                    .thenReturn(savedEvent);

            // 4. Stub: when building the EventResponse, the mapper calls sumLockedTickets
            when(registrationRepository.sumLockedTicketsForEvent(any(UUID.class)))
                    .thenReturn(0);

            // 5. Stub: bannerUrl is null so presignedUrl is never called, but s3Service
            //    is still injected — no stub needed if the method is not called.

            // ──────────────────────────────────────────────────────────────────
            //  ACT — call the real method we're testing
            // ──────────────────────────────────────────────────────────────────
            EventResponse response = eventService.createEvent(validRequest, "admin@test.com");

            // ──────────────────────────────────────────────────────────────────
            //  ⑧ ASSERTIONS
            //
            //  Assertions verify that the result is what we expect.
            //  If an assertion fails, the test fails and reports the mismatch.
            //
            //  Common JUnit 5 assertions:
            //   • assertNotNull(obj)             — obj must not be null
            //   • assertEquals(expected, actual) — must be equal
            //   • assertTrue(condition)           — must be true
            //   • assertFalse(condition)          — must be false
            //
            //  All come from: import static org.junit.jupiter.api.Assertions.*;
            // ──────────────────────────────────────────────────────────────────
            assertNotNull(response);
            assertEquals(EventStatus.DRAFT, response.getStatus());
            assertEquals("Tech Conference 2026", response.getTitle());

            // ──────────────────────────────────────────────────────────────────
            //  ⑨ verify(...)  —  INTERACTION VERIFICATION
            //
            //  Assertions check the RETURN VALUE. verify() checks that the
            //  mock's method was actually CALLED (and how many times).
            //
            //  SYNTAX:
            //    verify(mockObject).method(argument)         — called exactly once
            //    verify(mockObject, times(2)).method(arg)    — called exactly 2 times
            //    verify(mockObject, never()).method(arg)     — never called
            //    verify(mockObject, atLeastOnce()).method()  — called ≥ 1 time
            //
            //  WHY use verify?
            //  • Even if the return value looks right, maybe the save() was never called.
            //  • verify() catches those "the code returned a cached value and never hit the DB" bugs.
            // ──────────────────────────────────────────────────────────────────

            // Verify the service looked up the admin exactly once
            verify(userRepository, times(1)).findByEmail("admin@test.com");

            // Verify the event was saved to the repository exactly once
            verify(eventRepository, times(1)).save(any(Event.class));
        }

        @Test
        @DisplayName("✅ Created event should have DRAFT status by default")
        void createdEventShouldBeInDraftStatus() {
            // ARRANGE
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(fakeAdmin));
            when(eventRepository.existsByUniqueEventLink(anyString())).thenReturn(false);

            Event savedEvent = buildFakeEvent(UUID.randomUUID(), EventStatus.DRAFT);
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(registrationRepository.sumLockedTicketsForEvent(any())).thenReturn(0);

            // ACT
            EventResponse response = eventService.createEvent(validRequest, "admin@test.com");

            // ASSERT — new events must always start as DRAFT
            assertEquals(EventStatus.DRAFT, response.getStatus(),
                    "A freshly created event should always be in DRAFT status");
        }

        @Test
        @DisplayName("❌ Should throw ResourceNotFoundException when admin email doesn't exist")
        void shouldThrowWhenAdminNotFound() {

            // ──────────────────────────────────────────────────────────────────
            //  ⑦ when(...).thenThrow(...)
            //
            //  You can also stub a mock to THROW an exception.
            //  This lets you test your error-handling code paths.
            // ──────────────────────────────────────────────────────────────────

            // Stub: admin email doesn't exist in the DB → return empty Optional
            when(userRepository.findByEmail("nobody@test.com"))
                    .thenReturn(Optional.empty());
            // The service then calls .orElseThrow(() -> new ResourceNotFoundException(...))
            // which will throw the exception — we don't need to stub it here.

            // ──────────────────────────────────────────────────────────────────
            //  ⑪ assertThrows(ExceptionType.class, () -> { ... })
            //
            //  assertThrows verifies that the lambda THROWS the expected exception.
            //  If the code does NOT throw, the test FAILS.
            //  It also RETURNS the exception so you can assert on its message!
            // ──────────────────────────────────────────────────────────────────
            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> eventService.createEvent(validRequest, "nobody@test.com"),
                    "Should throw ResourceNotFoundException when admin is not found"
            );

            // Assert on the exception's message for extra confidence
            assertTrue(ex.getMessage().contains("Admin not found"));

            // Verify that save() was NEVER called — we shouldn't persist anything
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Should pass the correct Event object to save() — ArgumentCaptor demo")
        void shouldPassCorrectEventDataToRepository() {

            // ──────────────────────────────────────────────────────────────────
            //  ⑩ ArgumentCaptor — inspecting what was passed to a mock
            //
            //  verify(repo).save(any(Event.class)) only checks that save() was called.
            //  But what if we want to inspect the ACTUAL Event object that was passed?
            //  That's what ArgumentCaptor is for!
            //
            //  Step 1: eventCaptor.capture() is used as the argument matcher in verify()
            //  Step 2: eventCaptor.getValue() gives you the object that was passed
            // ──────────────────────────────────────────────────────────────────

            // ARRANGE
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(fakeAdmin));
            when(eventRepository.existsByUniqueEventLink(anyString())).thenReturn(false);

            // We need save() to return something with an ID so the response mapper works
            when(eventRepository.save(eventCaptor.capture()))
                    .thenAnswer(invocation -> {
                        // Return whatever was passed in (simulates JPA save returning the entity)
                        Event e = invocation.getArgument(0);
                        return buildFakeEvent(UUID.randomUUID(), e.getStatus());
                    });

            when(registrationRepository.sumLockedTicketsForEvent(any())).thenReturn(0);

            // ACT
            eventService.createEvent(validRequest, "admin@test.com");

            // ASSERT — inspect the captured Event
            Event capturedEvent = eventCaptor.getValue();

            assertNotNull(capturedEvent, "An Event must have been passed to save()");
            assertEquals("Tech Conference 2026", capturedEvent.getTitle(),
                    "The event title should match the request");
            assertEquals(EventStatus.DRAFT, capturedEvent.getStatus(),
                    "A new event must be saved with DRAFT status");
            assertEquals(fakeAdmin, capturedEvent.getCreatedBy(),
                    "The event's createdBy should be the admin who created it");
            assertEquals(200, capturedEvent.getTotalCapacity(),
                    "Capacity should match the request");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("publishEvent()")
    class PublishEventTests {

        @Test
        @DisplayName("✅ Should publish a DRAFT event successfully")
        void shouldPublishDraftEvent() {
            // ARRANGE
            UUID eventId = UUID.randomUUID();
            Event draftEvent = buildFakeEvent(eventId, EventStatus.DRAFT);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(draftEvent);
            when(registrationRepository.sumLockedTicketsForEvent(any())).thenReturn(0);

            // ACT
            EventResponse response = eventService.publishEvent(eventId);

            // ASSERT
            assertEquals(EventStatus.PUBLISHED, response.getStatus());
            verify(eventRepository, times(1)).save(draftEvent);
        }

        @Test
        @DisplayName("❌ Should throw IllegalStateException when publishing a CANCELLED event")
        void shouldThrowWhenPublishingCancelledEvent() {
            // ARRANGE
            UUID eventId = UUID.randomUUID();
            Event cancelledEvent = buildFakeEvent(eventId, EventStatus.CANCELLED);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(cancelledEvent));

            // ACT + ASSERT
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> eventService.publishEvent(eventId)
            );

            assertEquals("Cannot publish a cancelled event", ex.getMessage());

            // CRITICAL: verify save() was NEVER called — we don't want to persist invalid state
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ Should throw ResourceNotFoundException for a non-existent event ID")
        void shouldThrowWhenEventIdNotFound() {
            // ARRANGE — repository returns empty, simulating "event not found"
            UUID missingId = UUID.randomUUID();
            when(eventRepository.findById(missingId)).thenReturn(Optional.empty());

            // ACT + ASSERT
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> eventService.publishEvent(missingId)
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cancelEvent()")
    class CancelEventTests {

        @Test
        @DisplayName("✅ Should cancel a PUBLISHED event")
        void shouldCancelPublishedEvent() {
            UUID eventId = UUID.randomUUID();
            Event publishedEvent = buildFakeEvent(eventId, EventStatus.PUBLISHED);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(publishedEvent);

            // cancelEvent returns void, so no response to assert on
            eventService.cancelEvent(eventId);

            // Use ArgumentCaptor to verify the status was set before saving
            verify(eventRepository).save(eventCaptor.capture());
            assertEquals(EventStatus.CANCELLED, eventCaptor.getValue().getStatus(),
                    "The event status must be CANCELLED before save() is called");
        }

        @Test
        @DisplayName("❌ Should throw IllegalStateException when cancelling a COMPLETED event")
        void shouldThrowWhenCancellingCompletedEvent() {
            UUID eventId = UUID.randomUUID();
            Event completedEvent = buildFakeEvent(eventId, EventStatus.COMPLETED);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(completedEvent));

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> eventService.cancelEvent(eventId)
            );

            assertEquals("Cannot cancel a completed event", ex.getMessage());
            verify(eventRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllEvents()")
    class GetAllEventsTests {

        @Test
        @DisplayName("✅ Should return all events as a mapped list")
        void shouldReturnAllEventsAsList() {
            // ARRANGE — simulate 3 events in the DB
            List<Event> fakeEvents = List.of(
                    buildFakeEvent(UUID.randomUUID(), EventStatus.PUBLISHED),
                    buildFakeEvent(UUID.randomUUID(), EventStatus.DRAFT),
                    buildFakeEvent(UUID.randomUUID(), EventStatus.CANCELLED)
            );
            when(eventRepository.findAllByOrderByEventDateDesc()).thenReturn(fakeEvents);
            when(registrationRepository.sumLockedTicketsForEvent(any())).thenReturn(0);
            // ──────────────────────────────────────────────────────────────────
            //  ⚠️  UnnecessaryStubbingException — A Lesson from Mockito Strict Mode
            //
            //  Mockito (via MockitoExtension) uses STRICT stubbing by default.
            //  If you stub something that is NEVER called during the test, it
            //  fails with UnnecessaryStubbingException.
            //
            //  WHY? Because unused stubs are "dead code" in tests — they make
            //  tests harder to read and can hide real bugs (e.g., you expected
            //  a method to be called, but it wasn't).
            //
            //  Our fake events have bannerUrl = null, so the service never
            //  calls s3Service.generatePresignedUrl(). We do NOT stub it here.
            //
            //  If you ever NEED to stub something that might not always be called
            //  (e.g., in a parameterised or shared setup), use lenient stubbing:
            //    lenient().when(s3Service.generatePresignedUrl(any(), any())).thenReturn("url");
            // ──────────────────────────────────────────────────────────────────

            // ACT
            var responses = eventService.getAllEvents();

            // ASSERT
            assertNotNull(responses);
            assertEquals(3, responses.size(), "Should return all 3 events");

            // Verify the repo method was called exactly once
            verify(eventRepository, times(1)).findAllByOrderByEventDateDesc();
        }

        @Test
        @DisplayName("✅ Should return empty list when there are no events")
        void shouldReturnEmptyListWhenNoEventsExist() {
            when(eventRepository.findAllByOrderByEventDateDesc()).thenReturn(List.of());

            var responses = eventService.getAllEvents();

            assertNotNull(responses);
            assertTrue(responses.isEmpty(), "Should return an empty list, not null");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getEventById()")
    class GetEventByIdTests {

        @Test
        @DisplayName("✅ Should return event response when event exists")
        void shouldReturnEventWhenFound() {
            UUID eventId = UUID.randomUUID();
            Event event = buildFakeEvent(eventId, EventStatus.PUBLISHED);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(registrationRepository.sumLockedTicketsForEvent(eventId)).thenReturn(50);

            EventResponse response = eventService.getEventById(eventId);

            assertNotNull(response);
            assertEquals(eventId, response.getId());
            assertEquals(50, response.getBookedCount(), "Booked count should be 50");
            assertEquals(150, response.getAvailableCount(), "Available = 200 - 50 = 150");
        }

        @Test
        @DisplayName("❌ Should throw when event ID doesn't exist")
        void shouldThrowWhenEventNotFound() {
            UUID missingId = UUID.randomUUID();
            when(eventRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> eventService.getEventById(missingId)
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BONUS DEMO: Showing more ArgumentMatcher patterns
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("ArgumentMatchers Showcase")
    class ArgumentMatchersDemo {

        @Test
        @DisplayName("🎓 Demonstrating different ArgumentMatcher types")
        void demonstratingArgumentMatchers() {
            UUID specificId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Event event = buildFakeEvent(specificId, EventStatus.DRAFT);

            // eq() — matches ONLY this exact value (uses .equals())
            when(eventRepository.findById(eq(specificId)))
                    .thenReturn(Optional.of(event));

            // any(UUID.class) — matches ANY UUID argument
            // We already showed this above. Here's a note on the rules:
            //
            // IMPORTANT RULE: In a single method call stub, either ALL arguments
            // must use matchers, or NONE should (you can't mix matchers with raw values).
            //
            // ❌ WRONG: when(repo.someMethod(anyString(), "literal")) ...
            // ✅ RIGHT: when(repo.someMethod(anyString(), eq("literal"))) ...

            // Testing it works with eq()
            Optional<Event> result = eventRepository.findById(specificId);
            assertTrue(result.isPresent());

            // Verify with a matcher
            verify(eventRepository).findById(eq(specificId));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPER — builds a fake Event for test setup
    //
    //  This is a "test factory method". Centralizing object construction here
    //  means if the Event class changes, you fix one place, not 20 tests.
    // ══════════════════════════════════════════════════════════════════════════
    private Event buildFakeEvent(UUID id, EventStatus status) {
        return Event.builder()
                .id(id)
                .title("Tech Conference 2026")
                .description("A great event about Java and Spring Boot")
                .category(EventCategory.CULTURAL)
                .eventDate(LocalDate.now().plusDays(30))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(21, 0))
                .registrationDeadline(LocalDateTime.now().plusDays(25))
                .venue("Main Auditorium, RIC")
                .totalCapacity(200)
                .maxTicketsPerMember(4)
                .freeTicketsPerRegistration(2)
                .ticketPrice(BigDecimal.valueOf(500))
                .platformFeePerTicket(BigDecimal.valueOf(20))
                .status(status)
                .uniqueEventLink("tech-conference-2026-abc123")
                .createdBy(fakeAdmin)
                .build();
    }
}
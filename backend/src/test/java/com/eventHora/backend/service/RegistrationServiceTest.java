package com.eventHora.backend.service;

import com.eventHora.backend.Enum.EventStatus;
import com.eventHora.backend.Enum.MemberType;
import com.eventHora.backend.Enum.PaymentPreference;
import com.eventHora.backend.dto.InitiateBookingRequest;
import com.eventHora.backend.dto.InitiateBookingResponse;
import com.eventHora.backend.exception.ResourceNotFoundException;
import com.eventHora.backend.model.Event;
import com.eventHora.backend.dto.MemberSession;
import com.eventHora.backend.repository.EventRepository;
import com.eventHora.backend.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RazorpayService razorpayService;

    @InjectMocks
    private RegistrationService registrationService;

    private InitiateBookingRequest initiateBookingRequest;
    private MemberSession validSession;
    private Event validEvent;

    private static final String SESSION_PREFIX = "session:";

//    @BeforeEach
//    void setUp() {
//
//        initiateBookingRequest = new InitiateBookingRequest();
//        initiateBookingRequest.setEventId(UUID.randomUUID());
//        initiateBookingRequest.setPaymentPreference(PaymentPreference.AT_GATE);
//        initiateBookingRequest.setQuantity(2);
//        initiateBookingRequest.setSessionToken("test-session-token");
//
//        validSession = MemberSession.builder()
//                .memberId("RIC-1001")
//                .memberType(MemberType.INDIAN)
//                .identifier("9876543210")
//                .build();
//
//        validEvent = Event.builder()
//                .id(initiateBookingRequest.getEventId())
//                .title("Test Event")
//                .status(EventStatus.PUBLISHED)
//                .registrationDeadline(LocalDateTime.now().plusDays(5))
//                .maxTicketsPerMember(4)
//                .totalCapacity(100)
//                .ticketPrice(BigDecimal.valueOf(100))
//                .platformFeePerTicket(BigDecimal.valueOf(10))
//                .build();
//    }
//
//    @Nested
//    class InitiateBookingTests {
//
//
//        @Test
//        void initiateBooking_success() {
//            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
//            when(valueOperations.get(SESSION_PREFIX + initiateBookingRequest.getSessionToken()))
//                    .thenReturn(validSession);
//            when(eventRepository.findById(initiateBookingRequest.getEventId()))
//                    .thenReturn(Optional.of(validEvent));
//            when(registrationRepository.sumLockedTicketsForEvent(validEvent.getId()))
//                    .thenReturn(10);
//            when(registrationRepository.findByMemberIdAndEventId(validSession.getMemberId(), validEvent.getId()))
//                    .thenReturn(Optional.empty());
//
//            InitiateBookingResponse response = registrationService.initiateBooking(initiateBookingRequest);
//
//            assertNotNull(response);
//            assertTrue(response.getMessage().contains("OTP sent to"));
//            assertEquals(300, response.getExpiresInSeconds());
//
//            verify(valueOperations, times(2)).set(anyString(), any(), any(java.time.Duration.class));
//        }
//
//        @Test
//        void initiateBooking_eventNotFound() {
//            when(valueOperations.get(SESSION_PREFIX + initiateBookingRequest.getSessionToken()))
//                    .thenReturn(validSession);
//            when(eventRepository.findById(initiateBookingRequest.getEventId()))
//                    .thenReturn(Optional.empty());
//
//            assertThrows(
//                    ResourceNotFoundException.class,
//                    () -> registrationService.initiateBooking(initiateBookingRequest)
//            );
//        }
//
//        @Test
//        void initiateBooking_memberNotFound() {
//            when(valueOperations.get(SESSION_PREFIX + initiateBookingRequest.getSessionToken()))
//                    .thenReturn(null);
//
//            assertThrows(
//                    BadCredentialsException.class,
//                    () -> registrationService.initiateBooking(initiateBookingRequest)
//            );
//        }
//    }
}
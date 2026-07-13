package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Stored in Redis under key "intent:{sessionToken}" with a 10-minute TTL.
 *
 * Locks the booking intent between /initiate and /verify-otp.
 * Using Redis prevents the frontend from changing quantity or eventId
 * between the two calls — the backend always uses what was locked here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingIntent implements Serializable {

    private UUID eventId;
    private int quantity;
    private PaymentPreference paymentPreference;
}

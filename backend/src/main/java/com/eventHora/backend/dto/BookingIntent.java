package com.eventHora.backend.dto;

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
 * Using Redis prevents the frontend from changing quantities or eventId
 * between the two calls — the backend always uses what was locked here.
 *
 * Payment preference is no longer stored here — member self-service bookings
 * always use online payment. Only the quantities need to be locked.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingIntent implements Serializable {

    private UUID eventId;
    private int memberQuantity;            // RIC-member tier seats locked
    private int guestQuantity;             // Guest tier seats locked
}

package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingIntent implements Serializable {

    private UUID eventId;
    private int quantity;
    private PaymentPreference paymentPreference;
}

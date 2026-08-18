package com.eventHora.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitiateBookingResponse {

    private String message;
    private int expiresInSeconds;
}

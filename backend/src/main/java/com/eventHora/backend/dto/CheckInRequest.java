package com.eventHora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckInRequest {

    @NotBlank(message = "Ticket reference is required")
    private String ticketReference;
}

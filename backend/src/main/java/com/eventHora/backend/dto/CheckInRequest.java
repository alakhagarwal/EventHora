package com.eventHora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for POST /api/staff/checkin
 *
 * The ticketReference is scanned from the QR code on the member's ticket.
 */
@Data
public class CheckInRequest {

    @NotBlank(message = "Ticket reference is required")
    private String ticketReference;
}

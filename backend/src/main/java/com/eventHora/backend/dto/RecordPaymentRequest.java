package com.eventHora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RecordPaymentRequest {

    @NotBlank(message = "Ticket reference is required")
    private String ticketReference;

    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "PAID|COMPLIMENTARY",
        message = "Action must be 'PAID' or 'COMPLIMENTARY'"
    )
    private String action;
}

package com.eventHora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for POST /api/staff/record-payment
 *
 * Used by STAFF to record cash or complimentary collection for a PAY_AT_GATE ticket.
 * Recording payment simultaneously checks the member in — one atomic action at the gate.
 */
@Data
public class RecordPaymentRequest {

    @NotBlank(message = "Ticket reference is required")
    private String ticketReference;

    /**
     * The action to take:
     *  - "PAID"          → member paid cash/card; status becomes CONFIRMED
     *  - "COMPLIMENTARY" → staff waived the fee; status becomes COMPLIMENTARY
     */
    @NotBlank(message = "Action is required")
    @Pattern(
        regexp = "PAID|COMPLIMENTARY",
        message = "Action must be 'PAID' or 'COMPLIMENTARY'"
    )
    private String action;
}

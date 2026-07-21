package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for POST /api/staff/checkin
 *
 * Returned to the staff member's screen after a successful or already-done check-in.
 * Contains enough detail to let the staff visually confirm the right person is entering.
 */
@Data
@Builder
public class CheckInResponse {

    private String ticketReference;        // e.g. "TKT-2026-AB12CD"
    private String memberId;               // e.g. "RIC-2024-04512" — for visual verification
    private String eventTitle;             // Shown on staff's screen to confirm correct event
    private int quantity;                  // Number of tickets (people) admitted on this scan
    private BigDecimal totalAmount;        // Amount paid (for gate reference)
    private PaymentStatus paymentStatus;   // CONFIRMED / FREE / PAY_AT_GATE / COMPLIMENTARY

    private boolean alreadyCheckedIn;      // true if this ticket was already scanned before
    private LocalDateTime checkedInAt;     // Timestamp of check-in (current if first scan, original if duplicate)

    private String message;                // Human-readable result, e.g. "Check-in successful" or "Already checked in"
}

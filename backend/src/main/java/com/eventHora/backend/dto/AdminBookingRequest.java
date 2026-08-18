package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.AdminBookingAction;
import com.eventHora.backend.Enum.MemberType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AdminBookingRequest {

    @NotBlank(message = "Member ID is required")
    private String memberId;

    @NotNull(message = "Member type is required")
    private MemberType memberType;

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @NotNull(message = "Action is required")
    private AdminBookingAction action;
}

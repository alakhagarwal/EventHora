package com.eventHora.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderMediaRequest {

    @NotEmpty(message = "Ordered IDs list cannot be empty")
    private List<UUID> orderedIds;
}

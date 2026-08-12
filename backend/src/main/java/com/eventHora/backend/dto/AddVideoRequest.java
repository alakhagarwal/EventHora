package com.eventHora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddVideoRequest {

    @NotBlank(message = "Video URL is required")
    private String url;

    private String caption;

    @NotNull(message = "Sort order is required")
    private Integer sortOrder;
}

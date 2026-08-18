package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.MemberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyMemberRequest {

    @NotBlank(message = "Member ID is required")
    private String memberId;

    @NotBlank(message = "Identifier is required")
    private String identifier;

    @NotNull(message = "Member type is required")
    private MemberType memberType;
}

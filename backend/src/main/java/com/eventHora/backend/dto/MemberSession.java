package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.MemberType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Represents the verified member session stored in Redis.
 * This ensures the backend knows exactly who is booking without trusting the frontend.
 */
@Data
@Builder
public class MemberSession implements Serializable {
    private String sessionToken;
    private String memberId;
    private String identifier;
    private MemberType memberType;
}

package com.eventHora.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String tokenType;     // always "Bearer"
    private String role;
    private String name;
    private String email;
}

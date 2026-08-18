package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.MemberType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSession implements Serializable {
    private String sessionToken;
    private String memberId;
    private String identifier;
    private MemberType memberType;
}

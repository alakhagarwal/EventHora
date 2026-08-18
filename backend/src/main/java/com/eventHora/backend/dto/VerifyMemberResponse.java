package com.eventHora.backend.dto;

import com.eventHora.backend.Enum.MemberType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyMemberResponse {
    private String sessionToken;
    private String memberId;
    private MemberType memberType;
    private String maskedIdentifier;

}

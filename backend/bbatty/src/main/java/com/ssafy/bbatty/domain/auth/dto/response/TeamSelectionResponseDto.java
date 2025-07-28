package com.ssafy.bbatty.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamSelectionResponseDto {
    private final Long teamId;
    private final String teamName;
    
    public static TeamSelectionResponseDto of(Long teamId, String teamName) {
        return TeamSelectionResponseDto.builder()
                .teamId(teamId)
                .teamName(teamName)
                .build();
    }
}
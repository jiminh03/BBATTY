package com.ssafy.bbatty.domain.user.dto.response;

import com.ssafy.bbatty.global.constants.BadgeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 개별 뱃지 응답 DTO
 */
@Getter
@Builder
public class BadgeResponse {
    private BadgeType badgeType;
    private String description;
    private boolean acquired;
    private LocalDateTime acquiredAt;
    
    public static BadgeResponse acquired(BadgeType badgeType, LocalDateTime acquiredAt) {
        return BadgeResponse.builder()
                .badgeType(badgeType)
                .description(badgeType.getDescription())
                .acquired(true)
                .acquiredAt(acquiredAt)
                .build();
    }
    
    public static BadgeResponse notAcquired(BadgeType badgeType) {
        return BadgeResponse.builder()
                .badgeType(badgeType)
                .description(badgeType.getDescription())
                .acquired(false)
                .acquiredAt(null)
                .build();
    }
}
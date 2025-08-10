package com.ssafy.bbatty.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 사용자 뱃지 전체 응답 DTO
 */
@Getter
@Builder
public class UserBadgeResponse {
    private Long userId;
    private List<BadgeCategoryResponse> badgeCategories;
    
    public static UserBadgeResponse of(Long userId, List<BadgeCategoryResponse> badgeCategories) {
        return UserBadgeResponse.builder()
                .userId(userId)
                .badgeCategories(badgeCategories)
                .build();
    }
}
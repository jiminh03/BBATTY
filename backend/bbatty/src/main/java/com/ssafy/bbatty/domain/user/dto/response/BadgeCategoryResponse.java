package com.ssafy.bbatty.domain.user.dto.response;

import com.ssafy.bbatty.global.constants.BadgeCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 뱃지 카테고리별 응답 DTO
 */
@Getter
@Builder
public class BadgeCategoryResponse {
    private BadgeCategory category;
    private String displayName;
    private String description;
    private String season; // 시즌 기반 뱃지의 경우에만 포함
    private List<BadgeResponse> badges;
    
    public static BadgeCategoryResponse stadium(List<BadgeResponse> badges) {
        return BadgeCategoryResponse.builder()
                .category(BadgeCategory.STADIUM_CONQUEST)
                .displayName(BadgeCategory.STADIUM_CONQUEST.getDisplayName())
                .description(BadgeCategory.STADIUM_CONQUEST.getDescription())
                .season(null)
                .badges(badges)
                .build();
    }
    
    public static BadgeCategoryResponse seasonal(BadgeCategory category, String season, List<BadgeResponse> badges) {
        return BadgeCategoryResponse.builder()
                .category(category)
                .displayName(category.getDisplayName())
                .description(category.getDescription())
                .season(season)
                .badges(badges)
                .build();
    }
}
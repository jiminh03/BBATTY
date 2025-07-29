package com.ssafy.bbatty.domain.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 매칭 채팅방 생성 요청
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatRoomCreateRequest {

    /** 채팅방 제목 */
    private String title;

    /** 경기 날짜 */
    private LocalDate gameDate;

    /** 경기 ID (선택한 경기) */
    private Long gameId;

    /** 최대 참여자 수 (최대 30명) */
    @Builder.Default
    private Integer maxParticipants = 30;

    /** 성별 제한 ("ALL", "MALE", "FEMALE") */
    @Builder.Default
    private String genderRestriction = "ALL";

    /** 최소 나이 */
    private Integer minAge;

    /** 최대 나이 */
    private Integer maxAge;

    /** 최소 승률 (%) */
    private Double minWinRate;

    /** 같은 팀 팬만 허용 여부 */
    @Builder.Default
    private Boolean sameTeamOnly = false;

    /** 대상 팀 ID (sameTeamOnly가 true일 때) */
    private Long targetTeamId;

    /**
     * 요청 유효성 검사
     */
    public boolean isValid() {
        if (title == null || title.trim().isEmpty()) return false;
        if (gameDate == null) return false;
        if (maxParticipants == null || maxParticipants < 2 || maxParticipants > 30) return false;
        if (minAge != null && maxAge != null && minAge > maxAge) return false;
        if (minWinRate != null && (minWinRate < 0 || minWinRate > 100)) return false;
        if (Boolean.TRUE.equals(sameTeamOnly) && targetTeamId == null) return false;

        return true;
    }

    /**
     * 사용자 조건 부합 여부 확인
     */
    public boolean matchesUserConditions(Long userId, String gender, Integer age, Double winRate, Long userTeamId) {
        // 성별 조건
        if (!"ALL".equals(genderRestriction)) {
            if (!genderRestriction.equals(gender)) return false;
        }

        // 나이 조건
        if (minAge != null && age < minAge) return false;
        if (maxAge != null && age > maxAge) return false;

        // 승률 조건
        if (minWinRate != null && winRate < minWinRate) return false;

        // 같은 팀 조건
        if (Boolean.TRUE.equals(sameTeamOnly)) {
            if (!targetTeamId.equals(userTeamId)) return false;
        }

        return true;
    }
}
package com.ssafy.bbatty.domain.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 매칭 채팅방 검색 조건
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatRoomSearchCriteria {

    /** 검색 키워드 (제목 검색) */
    private String keyword;

    /** 경기 날짜 (특정 날짜) */
    private LocalDate gameDate;

    /** 경기 날짜 시작 범위 */
    private LocalDate gameDateFrom;

    /** 경기 날짜 끝 범위 */
    private LocalDate gameDateTo;

    /** 팀 필터 (내 팀만 보기) */
    private Long teamId;

    /** 성별 필터 */
    private String genderFilter; // "ALL", "MALE", "FEMALE"

    /** 최소 나이 */
    private Integer minAge;

    /** 최대 나이 */
    private Integer maxAge;

    /** 최소 승률 */
    private Double minWinRate;

    /** 참여 가능한 방만 (자리 있는 방) */
    private Boolean availableOnly;

    /** 내가 참여 중인 방만 */
    private Boolean myRoomsOnly;

    /** 내가 참여 중인 방 제외 */
    private Boolean excludeMyRooms;

    /** 승리 요정 있는 방만 */
    private Boolean withWinningFairy;

    /** 정렬 기준 */
    private String sortBy; // "LATEST", "PARTICIPANTS", "AVG_WIN_RATE", "POPULARITY"

    /** 정렬 순서 */
    private String sortOrder; // "ASC", "DESC"

    /** 페이지 번호 (0부터 시작) */
    @Builder.Default
    private Integer page = 0;

    /** 페이지 크기 */
    @Builder.Default
    private Integer size = 20;

    /** 검색하는 사용자 ID (권한 체크용) */
    private Long searchUserId;

    /** 사용자 나이 (필터링용) */
    private Integer userAge;

    /** 사용자 성별 (필터링용) */
    private String userGender;

    /** 사용자 승률 (필터링용) */
    private Double userWinRate;

    /** 사용자 팀 ID (필터링용) */
    private Long userTeamId;

    /**
     * 기본 검색 조건 설정
     */
    public void setDefaults() {
        if (sortBy == null) {
            sortBy = "LATEST";
        }
        if (sortOrder == null) {
            sortOrder = "DESC";
        }
        if (availableOnly == null) {
            availableOnly = true; // 기본적으로 참여 가능한 방만
        }
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }

    /**
     * 사용자 조건 부합 여부 확인 필요한지
     */
    public boolean needsUserConditionCheck() {
        return userAge != null || userGender != null ||
                userWinRate != null || userTeamId != null;
    }

    /**
     * 날짜 범위 검색인지 확인
     */
    public boolean isDateRangeSearch() {
        return gameDateFrom != null && gameDateTo != null;
    }

    /**
     * 단일 날짜 검색인지 확인
     */
    public boolean isSingleDateSearch() {
        return gameDate != null;
    }

    /**
     * 텍스트 검색인지 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }
}
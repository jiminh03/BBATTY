package com.ssafy.bbatty.domain.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 매칭 채팅방 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatRoomInfo {

    /** 채팅방 ID */
    private String roomId;

    /** 채팅방 제목 */
    private String title;

    /** 방장 ID */
    private Long ownerId;

    /** 방장 닉네임 */
    private String ownerNickname;

    /** 방장 승률 */
    private Double ownerWinRate;

    /** 경기 날짜 */
    private LocalDate gameDate;

    /** 경기 ID */
    private Long gameId;

    /** 홈팀 이름 */
    private String homeTeamName;

    /** 어웨이팀 이름 */
    private String awayTeamName;

    /** 경기장 */
    private String stadium;

    /** 현재 참여자 수 */
    private Integer currentParticipants;

    /** 최대 참여자 수 */
    private Integer maxParticipants;

    /** 성별 제한 */
    private String genderRestriction; // "ALL", "MALE", "FEMALE"

    /** 최소 나이 */
    private Integer minAge;

    /** 최대 나이 */
    private Integer maxAge;

    /** 최소 승률 (%) */
    private Double minWinRate;

    /** 같은 팀 팬만 허용 여부 */
    private Boolean sameTeamOnly;

    /** 대상 팀 ID */
    private Long targetTeamId;

    /** 대상 팀 이름 */
    private String targetTeamName;

    /** 승리 요정 참여 수 */
    private Integer winningFairyCount;

    /** 채팅방 생성 시간 */
    private LocalDateTime createdAt;

    /** 참여자들의 평균 승률 */
    private Double participantsAverageWinRate;

    /** 활성화 상태 */
    private Boolean active;

    // ========== 비즈니스 로직만 (최소한) ==========

    /**
     * 참여 가능 여부 확인
     */
    public boolean canJoin() {
        return active != null && active &&
                currentParticipants != null && maxParticipants != null &&
                currentParticipants < maxParticipants &&
                gameDate != null && !gameDate.isBefore(LocalDate.now());
    }

    /**
     * 채팅방 가득참 여부
     */
    public boolean isFull() {
        return currentParticipants != null && maxParticipants != null &&
                currentParticipants >= maxParticipants;
    }
}
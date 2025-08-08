package com.ssafy.bbatty.domain.chat.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.ssafy.chat.common.util.KSTTimeUtil;

/**
 * 팀별 채팅방 정보 (개선 버전)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamChatRoomInfo {

    /** 채팅방 ID (예: "game_123_team_1") */
    private String roomId;

    /** 채팅방 이름 (예: "KIA 타이거즈 응원방") */
    private String roomName;

    /** 경기 ID */
    private Long gameId;

    /** 팀 ID */
    private Long teamId;

    /** 팀 이름 */
    private String teamName;

    /** 상대팀 ID */
    private Long opponentTeamId;

    /** 상대팀 이름 */
    private String opponentTeamName;

    /** 경기 일시 */
    private LocalDateTime gameDateTime;

    /** 경기장 */
    private String stadium;

    /** 현재 접속자 수 */
    private Integer currentUsers;

    /** 최대 접속자 수 (200명) */
    @Builder.Default
    private Integer maxUsers = 200;

    /** 활성화 상태 */
    private Boolean active;

    /** 더블헤더 여부 */
    private Boolean doubleHeader;

    /** 승리 요정 참여 수 */
    private Integer winningFairyCount;

    /** 홈/어웨이 구분 */
    private String teamType; // "HOME" or "AWAY"
    /**
     * 입장 가능 여부 확인
     */
    public boolean canJoin() {
        return active != null && active &&
                currentUsers != null && currentUsers < maxUsers;
    }

    /**
     * 채팅방 제목 생성 (더블헤더 포함)
     */
    public String getDisplayTitle() {
        String title = teamName + " 응원방";
        if (Boolean.TRUE.equals(doubleHeader)) {
            title += " (더블헤더)";
        }
        return title;
    }

    /**
     * 승리 요정 표시 텍스트
     */
    public String getWinningFairyText() {
        if (winningFairyCount != null && winningFairyCount > 0) {
            return String.format("승리 요정 %d명 함께하는 중 ✨", winningFairyCount);
        }
        return null;
    }

    /**
     * 채팅방 가득참 여부
     */
    public boolean isFull() {
        return currentUsers != null && maxUsers != null &&
                currentUsers >= maxUsers;
    }

    /**
     * 사용률 계산 (%)
     */
    public double getUsageRate() {
        if (currentUsers == null || maxUsers == null || maxUsers == 0) {
            return 0.0;
        }
        return (double) currentUsers / maxUsers * 100.0;
    }

    /**
     * 경기 시작까지 남은 시간 (분)
     */
    public long getMinutesUntilGame() {
        if (gameDateTime == null) {
            return 0;
        }
        LocalDateTime nowTime = KSTTimeUtil.now();
        if (gameDateTime.isBefore(nowTime)) {
            return 0; // 이미 시작됨
        }
        return java.time.Duration.between(nowTime, gameDateTime).toMinutes();
    }
}
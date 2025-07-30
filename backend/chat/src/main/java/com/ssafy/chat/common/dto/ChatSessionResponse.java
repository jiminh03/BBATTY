package com.ssafy.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 세션 생성 응답 DTO (클라이언트용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionResponse {

    /** 일회용 세션 토큰 */
    private String sessionToken;

    /** WebSocket 연결 URL */
    private String wsUrl;

    /** 사용자 닉네임 (채팅 표시용만) */
    private String userNickname;

    /** 사용자 승률 */
    private Double winRate;

    /** 직관 인증 여부 */
    private Boolean attendanceAuth;

    /** 팀 정보 */
    private TeamInfo teamInfo;

    /** 채팅방 정보 (매칭 채팅용) */
    private RoomInfo roomInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamInfo {
        private String teamId;
        private String teamName;
        private String teamLogo;
        private Integer teamRank;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomInfo {
        private String roomId;
        private String roomTitle;
        private Integer currentParticipants;
        private Integer maxParticipants;
    }
}
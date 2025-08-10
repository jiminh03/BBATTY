package com.ssafy.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatRoomCreateResponse {
        private String matchId;
        private Long gameId;
        private String matchTitle;
        private String matchDescription;
        private Long teamId;
        private int minAge;
        private int maxAge;
        private String genderCondition;
        private int maxParticipants;
        private int currentParticipants;
        
        // 최소 승률 조건
        private int minWinRate;

        // 생성 시간
        private String createdAt;

        // 매칭방 상태 (ACTIVE, FULL, CLOSED)
        private String status;

        // WebSocket 연결 URL (조회 시에만 포함)
        private String websocketUrl;
}

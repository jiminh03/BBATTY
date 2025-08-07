package com.ssafy.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 매칭 채팅방 입장 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatJoinResponse {

    // 세션 토큰
    private String sessionToken;

    // 사용자 ID
    private Long userId;

    // 매칭 ID
    private String matchId;

    // 토큰 만료 시간 (초)
    private long expiresIn;

    // WebSocket 접속 URL
    private String websocketUrl;

    private String nickname;
    private Long teamId;
    private String teamName;
}

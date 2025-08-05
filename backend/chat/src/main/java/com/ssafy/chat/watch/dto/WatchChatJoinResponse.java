package com.ssafy.chat.watch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 직관 채팅방 입장 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchChatJoinResponse {
    
    // 세션 토큰
    private String sessionToken;
    
    // 팀 ID
    private String teamId;
    
    // 게임 ID
    private String gameId;
    
    // 토큰 만료 시간 (초)
    private long expiresIn;
    
    // WebSocket 접속 URL
    private String websocketUrl;
}
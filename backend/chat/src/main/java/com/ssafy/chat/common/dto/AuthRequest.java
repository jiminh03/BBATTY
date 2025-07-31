package com.ssafy.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    
    /** JWT 토큰 */
    private String accessToken;
    
    /** 채팅방 타입 (game, match) */
    private String chatType;
    
    /** 채팅방 ID (teamId 또는 matchId) */
    private String roomId;
}
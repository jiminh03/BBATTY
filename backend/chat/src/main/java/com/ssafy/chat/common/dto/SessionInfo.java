package com.ssafy.chat.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 세션 정보 DTO
 * Redis에 저장되는 세션 정보의 타입 안전성 제공
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {
    
    /**
     * 세션 토큰
     */
    private String sessionToken;
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 사용자 닉네임
     */
    private String nickname;
    
    /**
     * 팀 ID
     */
    private Long teamId;
    
    /**
     * 팀명
     */
    private String teamName;
    
    /**
     * 채팅방 ID
     */
    private String roomId;
    
    /**
     * 채팅방 타입 (MATCH, WATCH)
     */
    private String roomType;
    
    /**
     * 게임 ID
     */
    private Long gameId;
    
    /**
     * 연결 시각 (timestamp)
     */
    private Long connectedAt;
    
    /**
     * 마지막 활동 시각 (timestamp)
     */
    private Long lastActivityAt;
    
    /**
     * 세션 유효 여부
     */
    private boolean isValid;
    
    /**
     * 세션의 유효성 검증
     */
    @JsonIgnore
    public boolean isValidSession() {
        return sessionToken != null && !sessionToken.trim().isEmpty()
               && userId != null && userId > 0
               && roomId != null && !roomId.trim().isEmpty()
               && roomType != null && !roomType.trim().isEmpty()
               && connectedAt != null
               && isValid;
    }
}
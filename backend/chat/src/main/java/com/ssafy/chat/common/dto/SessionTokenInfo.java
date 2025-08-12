package com.ssafy.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 세션 토큰 상세 정보 DTO
 * Redis에 저장되는 토큰의 상세 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionTokenInfo {
    
    /**
     * 토큰 문자열
     */
    private String token;
    
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
     * 토큰 발급 시각 (타임스탬프)
     */
    private Long issuedAt;
    
    /**
     * 토큰 만료 시각 (타임스탬프)
     */
    private Long expiresAt;
    
    /**
     * 추가 사용자 정보
     */
    private Integer age;
    private String gender;
    private Double winRate;
    private Boolean isWinFairy;
    private String profileImgUrl;
    
    /**
     * 토큰 만료 여부 확인
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
    
    /**
     * 토큰 유효 여부 확인
     * @return 유효하면 true
     */
    public boolean isValid() {
        return token != null && userId != null && roomId != null && !isExpired();
    }
    
    /**
     * 매칭 채팅방 토큰인지 확인
     * @return 매칭 채팅방이면 true
     */
    public boolean isMatchRoom() {
        return "MATCH".equals(roomType);
    }
    
    /**
     * 관전 채팅방 토큰인지 확인
     * @return 관전 채팅방이면 true
     */
    public boolean isWatchRoom() {
        return "WATCH".equals(roomType);
    }
    
    /**
     * 남은 유효 시간 (초 단위)
     * @return 남은 시간 (초)
     */
    public long getRemainingSeconds() {
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
}
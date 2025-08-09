package com.ssafy.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis에 저장되는 매칭 채팅방 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatRoom {
    
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
    
    // 생성 시간 (ISO String)
    private String createdAt;
    
    // 마지막 활동 시간
    private String lastActivityAt;
    
    // 매칭방 상태 (ACTIVE, FULL, CLOSED)
    private String status;
    
    // 방장 ID
    private String ownerId;
}
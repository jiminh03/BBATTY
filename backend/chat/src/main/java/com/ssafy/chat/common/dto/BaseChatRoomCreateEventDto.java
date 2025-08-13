package com.ssafy.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 채팅방 생성 이벤트 DTO 기본 클래스
 * 공통 필드들을 포함
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseChatRoomCreateEventDto {
    
    /**
     * 게임 ID
     */
    protected Long gameId;
    
    /**
     * 홈팀 ID
     */
    protected Long homeTeamId;
    
    /**
     * 홈팀 이름
     */
    protected String homeTeamName;
    
    /**
     * 원정팀 ID
     */
    protected Long awayTeamId;
    
    /**
     * 원정팀 이름
     */
    protected String awayTeamName;
    
    /**
     * 경기장
     */
    protected String stadium;
}
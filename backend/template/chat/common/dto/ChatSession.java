package com.ssafy.bbatty.domain.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 세션 정보 (서버 내부용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    /** 세션 토큰 (일회용) */
    private String sessionToken;

    /** 사용자 ID (서버 내부용 - 클라이언트 노출 금지) */
    private Long userId;

    /** 사용자 닉네임 (채팅 표시용) */
    private String userNickname;

    /** 팀 ID */
    private String teamId;

    /** 경기 ID */
    private Long gameId;

    /** 매칭 채팅방 ID (매칭 채팅용) */
    private String roomId;

    /** 세션 만료 시간 */
    private Long expiresAt;

    /**
     * 세션 만료 여부 확인
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
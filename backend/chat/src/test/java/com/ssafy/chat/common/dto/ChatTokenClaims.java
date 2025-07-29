package com.ssafy.bbatty.domain.chat.common.dto;

import com.ssafy.bbatty.domain.chat.common.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 토큰 클레임 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTokenClaims {

    /** 사용자 ID */
    private String userId;

    /** 채팅 타입 */
    private ChatType chatType;

    /** 토큰 발행 시간 */
    private Long issuedAt;

    /** 토큰 만료 시간 */
    private Long expiresAt;

    // === 게임 채팅 전용 클레임 ===
    /** 팀 ID (게임 채팅용) */
    private String teamId;

    /** 경기 ID (게임 채팅용) */
    private Long gameId;

    // === 매칭 채팅 전용 클레임 ===
    /** 채팅방 ID (매칭 채팅용) */
    private String roomId;

    /** 매칭 ID (매칭 채팅용) */
    private String matchId;

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt < System.currentTimeMillis();
    }

    /**
     * 게임 채팅 토큰인지 확인
     */
    public boolean isGameChat() {
        return ChatType.GAME.equals(chatType);
    }

    /**
     * 매칭 채팅 토큰인지 확인
     */
    public boolean isMatchChat() {
        return ChatType.MATCH.equals(chatType);
    }
}
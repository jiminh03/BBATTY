package com.ssafy.bbatty.domain.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 인증/인가 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAuthRequest {
    
    private String chatType;        // "MATCH" or "WATCH"
    private String action;          // "CREATE" or "JOIN"
    private String roomId;          // 채팅방 ID (참여 시)
    private Long matchId;           // 경기 ID
    private Long teamId;            // 팀 ID (매칭 채팅 생성 시)
    private String requestId;       // 요청 추적용 고유 ID
}
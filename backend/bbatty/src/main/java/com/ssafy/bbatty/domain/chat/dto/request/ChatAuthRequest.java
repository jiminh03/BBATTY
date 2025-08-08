package com.ssafy.bbatty.domain.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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
    private Long gameId;           // 게임 ID (matchId 대신)
    private Long teamId;           // 팀 ID
    private String requestId;      // 요청 추적용 고유 ID
    private Map<String, Object> roomInfo; // 채팅방 정보
    private Boolean allowOtherTeams; // 다른 팀과 같이 보기 여부 (매칭 채팅방용)
}

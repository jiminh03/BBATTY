package com.ssafy.chat.watch.controller;

import com.ssafy.chat.watch.handler.GameChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 게임 채팅 간단 API (테스트용)
 */
@RestController
@RequestMapping("/api/chat/game")
@RequiredArgsConstructor
@Slf4j
public class GameChatController {

    private final GameChatWebSocketHandler gameChatWebSocketHandler;

    /**
     * WebSocket 연결 정보 (테스트용)
     */
    @GetMapping("/connection-info")
    public ResponseEntity<Map<String, Object>> getConnectionInfo() {
        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("gameChatEndpoint", "/ws/game-chat");
        connectionInfo.put("requiredParams", Map.of(
                "userId", "사용자 ID",
                "userTeamId", "사용자 팀 ID (1-10)",
                "teamId", "채팅방 팀 ID (1-10)"
        ));
        connectionInfo.put("exampleUrl", "ws://localhost:8084/ws/game-chat?userId=1&userTeamId=1&teamId=1");
        connectionInfo.put("messageFormat", Map.of(
                "roomId", "game_123_team_1",
                "content", "메시지 내용"
        ));
        
        return ResponseEntity.ok(connectionInfo);
    }

    /**
     * 채팅방 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getChatStats() {
        try {
            Map<String, Object> stats = gameChatWebSocketHandler.getConnectionStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("채팅 통계 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("service", "game-chat");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
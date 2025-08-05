package com.ssafy.chat.watch.controller;

import com.ssafy.chat.watch.service.WatchChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 관전 채팅 API 컨트롤러
 */
@RestController
@RequestMapping("/api/chat/watch")
@RequiredArgsConstructor
@Slf4j
public class WatchChatController {

    private final WatchChatService watchChatService;

    /**
     * WebSocket 연결 정보 (테스트용)
     */
    @GetMapping("/connection-info")
    public ResponseEntity<Map<String, Object>> getConnectionInfo() {
        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("watchChatEndpoint", "/ws/watch-chat");
        connectionInfo.put("requiredParams", Map.of(
                "token", "세션 토큰",
                "teamId", "채팅방 팀 ID"
        ));
        connectionInfo.put("exampleUrl", "ws://localhost:8084/ws/watch-chat?token=your_session_token&teamId=team1");
        connectionInfo.put("messageFormat", Map.of(
                "type", "message",
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
            Map<String, Object> stats = new HashMap<>();
            stats.put("activeRooms", watchChatService.getActiveWatchRoomCount());
            stats.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("관전 채팅 통계 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 특정 채팅방 세션 수 조회
     */
    @GetMapping("/room/{roomId}/sessions")
    public ResponseEntity<Map<String, Object>> getRoomSessionCount(@PathVariable String roomId) {
        try {
            int sessionCount = watchChatService.getActiveSessionCount(roomId);
            Map<String, Object> result = new HashMap<>();
            result.put("roomId", roomId);
            result.put("sessionCount", sessionCount);
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("관전 채팅방 세션 수 조회 실패 - roomId: {}", roomId, e);
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
        health.put("service", "watch-chat");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
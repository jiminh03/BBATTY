package com.ssafy.chat.watch.controller;

import com.ssafy.chat.config.ChatProperties;
import com.ssafy.chat.global.response.ApiResponse;
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
    private final ChatProperties chatProperties;

    /**
     * WebSocket 연결 정보 (테스트용)
     */
    @GetMapping("/connection-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConnectionInfo() {
        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("watchChatEndpoint", "/ws/watch-chat");
        connectionInfo.put("requiredParams", Map.of(
                "token", "세션 토큰",
                "teamId", "채팅방 팀 ID"
        ));
        connectionInfo.put("exampleUrl", String.format("%s/ws/watch-chat?token=your_session_token&teamId=team1", chatProperties.getWebsocket().getBaseUrl()));
        connectionInfo.put("messageFormat", Map.of(
                "type", "message",
                "content", "메시지 내용"
        ));
        
        return ResponseEntity.ok(ApiResponse.success(connectionInfo));
    }

    /**
     * 채팅방 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeRooms", watchChatService.getActiveWatchRoomCount());
        stats.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 특정 채팅방 세션 수 조회
     */
    @GetMapping("/room/{roomId}/sessions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoomSessionCount(@PathVariable String roomId) {
        int sessionCount = watchChatService.getActiveSessionCount(roomId);
        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("sessionCount", sessionCount);
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 관전 채팅방 목록 조회
     */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWatchChatRooms() {
        Map<String, Object> result = watchChatService.getWatchChatRooms();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 특정 관전 채팅방 조회
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWatchChatRoom(@PathVariable String roomId) {
        Map<String, Object> roomInfo = watchChatService.getWatchChatRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success(roomInfo));
    }

    /**
     * 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("service", "watch-chat");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
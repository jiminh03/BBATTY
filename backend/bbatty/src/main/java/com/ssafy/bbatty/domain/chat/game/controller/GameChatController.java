package com.ssafy.bbatty.domain.chat.game.controller;

import com.ssafy.bbatty.domain.chat.game.dto.TeamChatRoomInfo;
import com.ssafy.bbatty.domain.chat.game.handler.GameChatWebSocketHandler;
import com.ssafy.bbatty.domain.chat.game.service.GameChatRoomService;
import com.ssafy.bbatty.domain.chat.game.service.GameChatTrafficService;
import com.ssafy.bbatty.domain.chat.game.service.GameChatUserService;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.constants.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 게임 채팅 테스트 및 관리 API
 */
@RestController
@RequestMapping("/api/chat/game")
@RequiredArgsConstructor
@Slf4j
public class GameChatController {

    private final GameChatRoomService gameChatRoomService;
    private final GameChatUserService gameChatUserService;
    private final GameChatTrafficService gameChatTrafficService;
    private final GameChatWebSocketHandler gameChatWebSocketHandler;

    /**
     * 게임 채팅방 생성 (테스트용)
     */
    @PostMapping("/rooms/{gameId}")
    public ResponseEntity<ApiResponse<List<TeamChatRoomInfo>>> createGameChatRooms(@PathVariable Long gameId) {
        try {
            List<TeamChatRoomInfo> chatRooms = gameChatRoomService.createTeamChatRooms(gameId);
            gameChatRoomService.activateGameChatRooms(gameId);
            
            log.info("게임 채팅방 생성 완료 - gameId: {}, 채팅방 수: {}", gameId, chatRooms.size());
            return ResponseEntity.ok(ApiResponse.success(chatRooms));
        } catch (Exception e) {
            log.error("게임 채팅방 생성 실패 - gameId: {}", gameId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<TeamChatRoomInfo>>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * 오늘의 활성 채팅방 목록 조회
     */
    @GetMapping("/rooms/today")
    public ResponseEntity<ApiResponse<List<TeamChatRoomInfo>>> getTodayRooms() {
        try {
            List<TeamChatRoomInfo> rooms = gameChatRoomService.getTodayTeamChatRooms();
            return ResponseEntity.ok(ApiResponse.success(rooms));
        } catch (Exception e) {
            log.error("오늘 채팅방 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<List<TeamChatRoomInfo>>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * 특정 팀 채팅방 통계 조회
     */
    @GetMapping("/rooms/{teamId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatRoomStats(@PathVariable String teamId) {
        try {
            Map<String, Object> stats = gameChatWebSocketHandler.getGameChatStats(teamId);
            
            // 실제 접속자 수로 totalUsers 수정 (일관성 확보)
            long actualUserCount = gameChatUserService.getConnectedUserCount(teamId);
            stats.put("totalUsers", actualUserCount);
            
            log.info("채팅방 통계 조회 - teamId: {}, totalUsers: {}", teamId, actualUserCount);
            
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("채팅방 통계 조회 실패 - teamId: {}", teamId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * 특정 팀 채팅방 사용자 목록 조회
     */
    @GetMapping("/rooms/{teamId}/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatRoomUsers(@PathVariable String teamId) {
        try {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("teamId", teamId);
            userInfo.put("connectedUsers", gameChatUserService.getConnectedUsers(teamId));
            userInfo.put("userCount", gameChatUserService.getConnectedUserCount(teamId));
            
            return ResponseEntity.ok(ApiResponse.success(userInfo));
        } catch (Exception e) {
            log.error("채팅방 사용자 조회 실패 - teamId: {}", teamId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * 특정 팀 채팅방 트래픽 조회
     */
    @GetMapping("/rooms/{teamId}/traffic")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatRoomTraffic(@PathVariable String teamId) {
        try {
            Map<String, Object> trafficInfo = new HashMap<>();
            trafficInfo.put("teamId", teamId);
            trafficInfo.put("currentTraffic", gameChatTrafficService.getCurrentTraffic(teamId));
            trafficInfo.put("isSpike", gameChatTrafficService.isTrafficSpike(teamId, 100L));
            
            return ResponseEntity.ok(ApiResponse.success(trafficInfo));
        } catch (Exception e) {
            log.error("채팅방 트래픽 조회 실패 - teamId: {}", teamId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * 특정 팀 채팅방 구독 시작 (테스트용)
     */
    @PostMapping("/rooms/{teamId}/subscribe")
    public ResponseEntity<ApiResponse<String>> subscribeToTeamChat(@PathVariable String teamId) {
        try {
            gameChatWebSocketHandler.subscribeToTeamChat(teamId);
            return ResponseEntity.ok(ApiResponse.success("구독 시작됨 - teamId: " + teamId));
        } catch (Exception e) {
            log.error("채팅방 구독 실패 - teamId: {}", teamId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<String>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * 특정 팀 채팅방 구독 해제 (테스트용)
     */
    @DeleteMapping("/rooms/{teamId}/subscribe")
    public ResponseEntity<ApiResponse<String>> unsubscribeFromTeamChat(@PathVariable String teamId) {
        try {
            gameChatWebSocketHandler.unsubscribeFromTeamChat(teamId);
            return ResponseEntity.ok(ApiResponse.success("구독 해제됨 - teamId: " + teamId));
        } catch (Exception e) {
            log.error("채팅방 구독 해제 실패 - teamId: {}", teamId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<String>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * 트래픽 리셋 (테스트용)
     */
    @DeleteMapping("/rooms/{teamId}/traffic")
    public ResponseEntity<ApiResponse<String>> resetTraffic(@PathVariable String teamId) {
        try {
            gameChatTrafficService.resetTraffic(teamId);
            return ResponseEntity.ok(ApiResponse.success("트래픽 리셋 완료 - teamId: " + teamId));
        } catch (Exception e) {
            log.error("트래픽 리셋 실패 - teamId: {}", teamId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<String>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * WebSocket 연결 정보 (테스트용)
     */
    @GetMapping("/connection-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConnectionInfo() {
        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("gameChatEndpoint", "/ws/game-chat");
        connectionInfo.put("requiredParams", Map.of(
                "userId", "사용자 ID (예: user123)",
                "teamId", "팀 ID (예: tigers, lions)"
        ));
        connectionInfo.put("exampleUrl", "ws://localhost:8080/ws/game-chat?userId=user123&teamId=tigers");
        connectionInfo.put("messageFormat", Map.of(
                "type", "message",
                "content", "메시지 내용"
        ));
        
        return ResponseEntity.ok(ApiResponse.success(connectionInfo));
    }

    /**
     * 채팅방 활성화/비활성화 (테스트용)
     */
    @PutMapping("/rooms/{gameId}/status")
    public ResponseEntity<ApiResponse<String>> updateRoomStatus(
            @PathVariable Long gameId,
            @RequestParam boolean active) {
        try {
            if (active) {
                gameChatRoomService.activateGameChatRooms(gameId);
                return ResponseEntity.ok(ApiResponse.success("채팅방 활성화 완료 - gameId: " + gameId));
            } else {
                gameChatRoomService.deactivateGameChatRooms(gameId);
                return ResponseEntity.ok(ApiResponse.success("채팅방 비활성화 완료 - gameId: " + gameId));
            }
        } catch (Exception e) {
            log.error("채팅방 상태 변경 실패 - gameId: {}", gameId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<String>fail(ErrorCode.SERVER_ERROR, null));
        }
    }
}
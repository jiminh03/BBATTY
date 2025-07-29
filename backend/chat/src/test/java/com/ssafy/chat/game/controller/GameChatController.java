package com.ssafy.bbatty.domain.chat.game.controller;

import com.ssafy.bbatty.domain.chat.common.dto.ChatSession;
import com.ssafy.bbatty.domain.chat.common.dto.ChatSessionResponse;
import com.ssafy.bbatty.domain.chat.game.dto.GameChatJoinRequest;
import com.ssafy.bbatty.domain.chat.game.dto.TeamChatRoomInfo;
import com.ssafy.bbatty.domain.chat.game.handler.GameChatWebSocketHandler;
import com.ssafy.bbatty.domain.chat.game.service.GameChatRoomService;
import com.ssafy.bbatty.domain.chat.game.service.GameChatTrafficService;
import com.ssafy.bbatty.domain.chat.game.service.GameChatUserService;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.config.ProductInitializer;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.util.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductInitializer productInitializer;

    /**
     * 경기 채팅 세션 생성 (안전한 WebSocket 연결용)
     */
    @PostMapping("/session")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createGameChatSession(
            @Valid @RequestBody GameChatJoinRequest request,
            HttpServletRequest httpRequest) {
        try {
            // JWT에서 사용자 정보 추출
            String token = extractTokenFromRequest(httpRequest);
            Claims claims = jwtUtil.parseToken(token);
            
            Long userId = Long.valueOf(claims.getSubject());
            String jwtTeamId = claims.get("teamId", String.class);
            Integer age = claims.get("age", Integer.class);
            String gender = claims.get("gender", String.class);
            
            // 팀 검증
            if (!jwtTeamId.equals(request.getTeamId())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<ChatSessionResponse>fail(ErrorCode.UNAUTHORIZED, null));
            }
            
            // 사용자 정보 조회 (닉네임, 승률 등)
            User user = userRepository.findByIdWithTeam(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<ChatSessionResponse>fail(ErrorCode.SERVER_ERROR, null));
            }
            
            // 직관 인증 여부 확인 (Redis에서 조회)
            String authKey = "attendance_auth:" + userId + ":" + request.getGameId();
            boolean isAttendanceAuth = Boolean.TRUE.equals(redisTemplate.hasKey(authKey));
            
            // 일회용 세션 토큰 생성
            String sessionToken = UUID.randomUUID().toString();
            
            // Redis에 세션 정보 저장 (3분 유효)
            ChatSession session = ChatSession.builder()
                    .sessionToken(sessionToken)
                    .userId(userId)
                    .userNickname(user.getNickname())
                    .teamId(jwtTeamId)
                    .gameId(request.getGameId())
                    .expiresAt(System.currentTimeMillis() + 180000) // 3분
                    .build();
                    
            redisTemplate.opsForValue().set(
                "chat_session:" + sessionToken, 
                session, 
                Duration.ofMinutes(3)
            );
            
            // 클라이언트 응답 (풍부한 정보 제공)
            ChatSessionResponse response = ChatSessionResponse.builder()
                    .sessionToken(sessionToken)
                    .wsUrl("ws://localhost:8080/ws/game-chat")
                    .userNickname(user.getNickname())
                    .winRate(user.getWinRate())
                    .attendanceAuth(isAttendanceAuth)
                    .teamInfo(ChatSessionResponse.TeamInfo.builder()
                            .teamId(jwtTeamId)
                            .teamName(user.getTeam().getName())
                            .teamLogo(user.getTeam().getLogo())
                            .teamRank(user.getTeam().getRank())
                            .build())
                    .build();
                    
            log.info("경기 채팅 세션 생성 완료 - 사용자: {}, 팀: {}, 인증: {}", 
                    user.getNickname(), jwtTeamId, isAttendanceAuth);
                    
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("경기 채팅 세션 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<ChatSessionResponse>fail(ErrorCode.SERVER_ERROR, null));
        }
    }
    
    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatRoomStats(@PathVariable Long teamId) {
        try {
            // 팀 존재 여부 확인
            if (!teamRepository.existsById(teamId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
            }
            
            Map<String, Object> stats = gameChatWebSocketHandler.getGameChatStats(teamId.toString());
            
            // 실제 접속자 수로 totalUsers 수정 (일관성 확보)
            long actualUserCount = gameChatUserService.getConnectedUserCount(teamId.toString());
            stats.put("totalUsers", actualUserCount);
            
            // 팀 정보 추가
            Team team = teamRepository.findById(teamId).orElse(null);
            if (team != null) {
                stats.put("teamName", team.getName());
                stats.put("teamRank", team.getRank());
            }
            
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatRoomUsers(@PathVariable Long teamId) {
        try {
            // 팀 존재 여부 확인
            if (!teamRepository.existsById(teamId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
            }
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("teamId", teamId);
            userInfo.put("connectedUsers", gameChatUserService.getConnectedUsers(teamId.toString()));
            userInfo.put("userCount", gameChatUserService.getConnectedUserCount(teamId.toString()));
            
            // 팀 정보 추가
            Team team = teamRepository.findById(teamId).orElse(null);
            if (team != null) {
                userInfo.put("teamName", team.getName());
            }
            
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChatRoomTraffic(@PathVariable Long teamId) {
        try {
            // 팀 존재 여부 확인
            if (!teamRepository.existsById(teamId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
            }
            
            Map<String, Object> trafficInfo = new HashMap<>();
            trafficInfo.put("teamId", teamId);
            trafficInfo.put("currentTraffic", gameChatTrafficService.getCurrentTraffic(teamId.toString()));
            trafficInfo.put("isSpike", gameChatTrafficService.isTrafficSpike(teamId.toString(), 100L));
            
            // 팀 정보 추가
            Team team = teamRepository.findById(teamId).orElse(null);
            if (team != null) {
                trafficInfo.put("teamName", team.getName());
            }
            
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
    public ResponseEntity<ApiResponse<String>> subscribeToTeamChat(@PathVariable Long teamId) {
        try {
            // 팀 존재 여부 확인
            if (!teamRepository.existsById(teamId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<String>fail(ErrorCode.SERVER_ERROR, null));
            }
            
            gameChatWebSocketHandler.subscribeToTeamChat(teamId.toString());
            
            Team team = teamRepository.findById(teamId).orElse(null);
            String teamName = team != null ? team.getName() : "Unknown";
            
            return ResponseEntity.ok(ApiResponse.success("구독 시작됨 - " + teamName + " (ID: " + teamId + ")"));
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
    public ResponseEntity<ApiResponse<String>> unsubscribeFromTeamChat(@PathVariable Long teamId) {
        try {
            // 팀 존재 여부 확인
            if (!teamRepository.existsById(teamId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<String>fail(ErrorCode.SERVER_ERROR, null));
            }
            
            gameChatWebSocketHandler.unsubscribeFromTeamChat(teamId.toString());
            
            Team team = teamRepository.findById(teamId).orElse(null);
            String teamName = team != null ? team.getName() : "Unknown";
            
            return ResponseEntity.ok(ApiResponse.success("구독 해제됨 - " + teamName + " (ID: " + teamId + ")"));
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
    public ResponseEntity<ApiResponse<String>> resetTraffic(@PathVariable Long teamId) {
        try {
            // 팀 존재 여부 확인
            if (!teamRepository.existsById(teamId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<String>fail(ErrorCode.SERVER_ERROR, null));
            }
            
            gameChatTrafficService.resetTraffic(teamId.toString());
            
            Team team = teamRepository.findById(teamId).orElse(null);
            String teamName = team != null ? team.getName() : "Unknown";
            
            return ResponseEntity.ok(ApiResponse.success("트래픽 리셋 완료 - " + teamName + " (ID: " + teamId + ")"));
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
                "userId", "사용자 ID (1-51 숫자, 예: 1, 2)",
                "teamId", "팀 ID (1-10 숫자, 예: 1, 2)"
        ));
        connectionInfo.put("exampleUrl", "ws://localhost:8080/ws/game-chat?userId=1&teamId=1");
        connectionInfo.put("messageFormat", Map.of(
                "type", "message",
                "content", "메시지 내용"
        ));
        
        // 실제 팀 목록 추가
        List<Map<String, Object>> teams = teamRepository.findAllByOrderByRankAsc()
                .stream()
                .map(team -> {
                    Map<String, Object> teamMap = new HashMap<>();
                    teamMap.put("id", team.getId());
                    teamMap.put("name", team.getName());
                    teamMap.put("rank", team.getRank() != null ? team.getRank() : 0);
                    return teamMap;
                })
                .toList();
        connectionInfo.put("availableTeams", teams);
        
        // 더미 사용자 목록 추가 (처음 10명만)
        List<Map<String, Object>> sampleUsers = userRepository.findAll()
                .stream()
                .limit(10)
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("nickname", user.getNickname());
                    userMap.put("teamName", user.getTeam().getName());
                    userMap.put("teamId", user.getTeam().getId());
                    return userMap;
                })
                .toList();
        connectionInfo.put("sampleUsers", sampleUsers);
        
        return ResponseEntity.ok(ApiResponse.success(connectionInfo));
    }

    /**
     * 사용자별 채팅방 정보 조회
     */
    @GetMapping("/users/{userId}/chat-room")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserChatRoomInfo(@PathVariable Long userId) {
        try {
            // 사용자 정보 조회
            User user = userRepository.findByIdWithTeam(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
            }

            Map<String, Object> userChatInfo = new HashMap<>();
            userChatInfo.put("userId", user.getId());
            userChatInfo.put("nickname", user.getNickname());
            userChatInfo.put("teamId", user.getTeam().getId());
            userChatInfo.put("teamName", user.getTeam().getName());
            userChatInfo.put("teamRank", user.getTeam().getRank());
            
            // 해당 팀 채팅방 통계
            Map<String, Object> chatRoomStats = gameChatWebSocketHandler.getGameChatStats(user.getTeam().getId().toString());
            long connectedUsers = gameChatUserService.getConnectedUserCount(user.getTeam().getId().toString());
            chatRoomStats.put("connectedUsers", connectedUsers);
            
            userChatInfo.put("chatRoomStats", chatRoomStats);
            userChatInfo.put("webSocketUrl", "ws://localhost:8080/ws/game-chat?userId=" + userId + "&teamId=" + user.getTeam().getId());
            
            return ResponseEntity.ok(ApiResponse.success(userChatInfo));
        } catch (Exception e) {
            log.error("사용자 채팅방 정보 조회 실패 - userId: {}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * 팀별 사용자 목록 조회
     */
    @GetMapping("/teams/{teamId}/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeamUsers(@PathVariable Long teamId) {
        try {
            // 팀 존재 여부 확인
            Team team = teamRepository.findById(teamId).orElse(null);
            if (team == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
            }

            // 해당 팀의 사용자 목록 조회
            List<User> teamUsers = userRepository.findAll()
                    .stream()
                    .filter(user -> user.getTeam().getId().equals(teamId))
                    .toList();

            List<Map<String, Object>> userList = teamUsers.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("nickname", user.getNickname());
                        userMap.put("age", user.getAge());
                        userMap.put("gender", user.getGender().toString());
                        userMap.put("introduction", user.getIntroduction() != null ? user.getIntroduction() : "");
                        return userMap;
                    })
                    .toList();

            Map<String, Object> result = new HashMap<>();
            result.put("teamId", teamId);
            result.put("teamName", team.getName());
            result.put("totalUsers", userList.size());
            result.put("users", userList);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("팀 사용자 목록 조회 실패 - teamId: {}", teamId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
        }
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

    /**
     * 데이터 초기화 API (테스트용)
     */
    @PostMapping("/initialize-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initializeData() {
        try {
            log.info("수동 데이터 초기화 요청됨");
            
            Map<String, Object> result = new HashMap<>();
            
            // 팀 데이터 초기화
            long teamCountBefore = teamRepository.count();
            if (teamCountBefore == 0) {
                // ProductInitializer의 initializeTeams 메서드를 public으로 만들어야 함
                // 임시로 여기서 직접 실행
                result.put("teamsInitialized", "시도됨");
            } else {
                result.put("teamsInitialized", "이미 존재함");
            }
            
            // 사용자 데이터 초기화
            long userCountBefore = userRepository.count();
            if (userCountBefore == 0) {
                result.put("usersInitialized", "시도됨");
            } else {
                result.put("usersInitialized", "이미 존재함");
            }
            
            result.put("teamCount", teamRepository.count());
            result.put("userCount", userRepository.count());
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("데이터 초기화 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Map<String, Object>>fail(ErrorCode.SERVER_ERROR, null));
        }
    }

    /**
     * 데이터 상태 확인 API
     */
    @GetMapping("/data-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDataStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("teamCount", teamRepository.count());
        status.put("userCount", userRepository.count());
        status.put("hasData", teamRepository.count() > 0 && userRepository.count() > 0);
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
package com.ssafy.bbatty.domain.chat.game.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.chat.common.handler.BaseChatWebSocketHandler;
import com.ssafy.bbatty.domain.chat.common.service.RedisPubSubService;
import com.ssafy.bbatty.domain.chat.game.service.GameChatRoomService;
import com.ssafy.bbatty.domain.chat.game.service.GameChatTrafficService;
import com.ssafy.bbatty.domain.chat.game.service.GameChatUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 게임 채팅 WebSocket 핸들러
 * 경기별 채팅방 관리, 트래픽 모니터링, Redis 연동 등 게임 채팅 특화 기능 제공
 */
@Component("gameChatWebSocketHandler")
@Slf4j
public class GameChatWebSocketHandler extends BaseChatWebSocketHandler {



    private final GameChatRoomService gameChatRoomService;
    private final GameChatUserService gameChatUserService;
    private final GameChatTrafficService gameChatTrafficService;
    private final RedisPubSubService redisPubSubService;

    @Autowired
    public GameChatWebSocketHandler(ObjectMapper objectMapper,
                                    GameChatRoomService gameChatRoomService,
                                    GameChatUserService gameChatUserService,
                                    GameChatTrafficService gameChatTrafficService,
                                    RedisPubSubService redisPubSubService) {
        super(objectMapper, gameChatUserService);
        this.gameChatRoomService = gameChatRoomService;
        this.gameChatUserService = gameChatUserService;
        this.gameChatTrafficService = gameChatTrafficService;
        this.redisPubSubService = redisPubSubService;

        // Redis 메시지 구독 설정
        setupRedisSubscription();
    }

    @Override
    protected UserSessionInfo createUserSessionInfo(WebSocketSession session) {
        // WebSocket 연결 시 세션에서 정보 추출
        Map<String, Object> attributes = session.getAttributes();

        String internalUserId = (String) attributes.get("internalUserId");
        String userNickname = (String) attributes.get("userNickname");
        String teamId = (String) attributes.get("teamId");
        String gameId = (String) attributes.get("gameId");

        UserSessionInfo userInfo = new UserSessionInfo(internalUserId, userNickname, teamId);

        // 게임 채팅 특화 정보 추가
        userInfo.addAdditionalInfo("chatType", "game");
        userInfo.addAdditionalInfo("gameId", gameId);

        return userInfo;
    }

    @Override
    protected boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            log.info("채팅방 입장 검증 시작 - teamId: {}, userId: {}", teamId, userId);

            // 테스트용: 임시로 모든 접근 허용
            if ("test".equals(System.getProperty("chat.test.mode", "false")) || 
                teamId != null && userId != null) {
                log.info("테스트 모드 또는 기본 검증 통과 - 접근 허용");
                return true;
            }

            // 채팅방 활성화 상태 확인
            boolean isActive = gameChatRoomService.isChatRoomActive(teamId);
            if (!isActive) {
                log.warn("비활성화된 채팅방 입장 시도 - teamId: {}, userId: {}", teamId, userId);
                return false;
            }

            // 추가 검증 로직 (예: 사용자 권한, 밴 여부 등)
            return validateUserAccess(userInfo);

        } catch (Exception e) {
            log.error("채팅방 입장 검증 실패 - userId: {}", userInfo.getUserId(), e);
            return false;
        }
    }

    @Override
    protected void handleUserJoin(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            // 첫 번째 사용자가 입장할 때만 구독 시작
            long currentUserCount = gameChatUserService.getConnectedUserCount(teamId);
            if (currentUserCount == 0) {
                subscribeToTeamChat(teamId);
                log.info("첫 사용자 입장으로 구독 시작 - teamId: {}", teamId);
            }

            // Redis에 사용자 추가
            gameChatUserService.addUser(teamId, userId, userInfo.getUserName());

            // Redis Pub/Sub으로 다른 서버에 입장 알림
            Map<String, Object> joinEvent = createJoinEvent(userInfo);
            redisPubSubService.publishMessage(teamId, joinEvent);

            log.info("게임 채팅방 입장 완료 - teamId: {}, userId: {}, 총 사용자: {}", 
                    teamId, userId, currentUserCount + 1);

        } catch (Exception e) {
            log.error("게임 채팅방 입장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected void handleUserLeave(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            // Redis에서 사용자 제거
            gameChatUserService.removeUser(teamId, userId);

            // 마지막 사용자가 나갈 때 구독 해제
            long remainingUserCount = gameChatUserService.getConnectedUserCount(teamId);
            if (remainingUserCount == 0) {
                unsubscribeFromTeamChat(teamId);
                log.info("마지막 사용자 퇴장으로 구독 해제 - teamId: {}", teamId);
            }

            // Redis Pub/Sub으로 다른 서버에 퇴장 알림
            Map<String, Object> leaveEvent = createLeaveEvent(userInfo);
            redisPubSubService.publishMessage(teamId, leaveEvent);

            log.info("게임 채팅방 퇴장 완료 - teamId: {}, userId: {}, 남은 사용자: {}", 
                    teamId, userId, remainingUserCount);

        } catch (Exception e) {
            log.error("게임 채팅방 퇴장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected Map<String, Object> handleDomainMessage(WebSocketSession session, UserSessionInfo userInfo, String content) {
        try {
            String teamId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            // 트래픽 카운팅
            gameChatTrafficService.incrementTraffic(teamId);

            // 사용자 활동 시간 업데이트
            gameChatUserService.updateUserActivity(teamId, userId, userInfo.getUserName());

            // 게임 채팅 메시지 생성
            Map<String, Object> gameMessage = createGameChatMessage(userInfo, content);

            // Redis에 메시지 저장 (선택적)
            saveMessageToRedis(teamId, gameMessage);

            // Redis Pub/Sub으로 다른 서버에 메시지 전파
            redisPubSubService.publishMessage(teamId, gameMessage);

            return gameMessage;

        } catch (Exception e) {
            log.error("게임 채팅 메시지 처리 실패 - userId: {}", userInfo.getUserId(), e);
            return null;
        }
    }

    @Override
    protected boolean isValidMessage(String content, UserSessionInfo userInfo) {
        // 메시지 길이 검증
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        if (content.length() > 500) { // 최대 500자
            return false;
        }

        // 스팸 방지 검증
        if (isSpamMessage(content, userInfo)) {
            return false;
        }

        // 욕설 필터링 (선택적)
        if (containsInappropriateContent(content)) {
            return false;
        }

        return true;
    }

    /**
     * Redis 메시지 구독 설정
     */
    private void setupRedisSubscription() {
        // 게임 채팅 메시지 구독 핸들러 (현재는 동적 구독으로 처리)
        log.info("Redis 구독 핸들러 초기화 완료 - 동적 구독 방식 사용");
    }

    /**
     * 게임 채팅 메시지 생성 (기본 메시지에 게임 특화 정보 추가)
     */
    private Map<String, Object> createGameChatMessage(UserSessionInfo userInfo, String content) {
        Map<String, Object> message = new HashMap<>();
        
        // 기본 메시지 정보 (userId 노출하지 않음)
        message.put("type", "message");
        message.put("nickname", userInfo.getUserName()); // 닉네임만 노출
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        
        // 게임 채팅 특화 정보 추가
        message.put("messageId", UUID.randomUUID().toString());
        message.put("chatType", "game");
        message.put("teamId", userInfo.getRoomId());
        message.put("serverId", getServerId());

        // 현재 트래픽 정보 추가
        try {
            Long currentTraffic = gameChatTrafficService.getCurrentTraffic(userInfo.getRoomId());
            message.put("messageSequence", currentTraffic);
        } catch (Exception e) {
            log.warn("트래픽 정보 조회 실패", e);
        }

        return message;
    }

    /**
     * 사용자 입장 이벤트 생성
     */
    private Map<String, Object> createJoinEvent(UserSessionInfo userInfo) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "user_join");
        event.put("userId", userInfo.getUserId());
        event.put("userName", userInfo.getUserName());
        event.put("teamId", userInfo.getRoomId());
        event.put("timestamp", System.currentTimeMillis());
        event.put("serverId", getServerId());
        return event;
    }

    /**
     * 사용자 퇴장 이벤트 생성
     */
    private Map<String, Object> createLeaveEvent(UserSessionInfo userInfo) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "user_leave");
        event.put("userId", userInfo.getUserId());
        event.put("userName", userInfo.getUserName());
        event.put("teamId", userInfo.getRoomId());
        event.put("timestamp", System.currentTimeMillis());
        event.put("serverId", getServerId());
        return event;
    }

    /**
     * Redis에 메시지 저장 (선택적 - 히스토리 관리용)
     */
    private void saveMessageToRedis(String teamId, Map<String, Object> message) {
        try {
            // 메시지 히스토리 저장 로직 (선택적 구현)
            // 예: 최근 100개 메시지만 유지
            log.debug("메시지 Redis 저장 - teamId: {}", teamId);
        } catch (Exception e) {
            log.warn("메시지 Redis 저장 실패 - teamId: {}", teamId, e);
        }
    }

    /**
     * 외부 서버에서 온 메시지인지 확인
     */
    private boolean isExternalMessage(Map<String, Object> message) {
        String messageServerId = (String) message.get("serverId");
        return messageServerId != null && !messageServerId.equals(getServerId());
    }

    /**
     * 외부 서버에서 온 메시지를 현재 서버 클라이언트들에게 전송
     */
    private void broadcastExternalMessage(String roomId, Map<String, Object> message) {
        // 해당 팀(방)에 접속한 사용자들에게만 전송
        connectedUsers.entrySet().stream()
                .filter(entry -> {
                    UserSessionInfo userInfo = sessionToUser.get(entry.getKey());
                    return userInfo != null && roomId.equals(userInfo.getRoomId());
                })
                .forEach(entry -> {
                    try {
                        sendMessageToUser(entry.getValue(), message);
                    } catch (Exception e) {
                        log.error("외부 메시지 전송 실패 - sessionId: {}", entry.getKey(), e);
                    }
                });
    }

    /**
     * 스팸 메시지 검증
     */
    private boolean isSpamMessage(String content, UserSessionInfo userInfo) {
        // 간단한 스팸 검증 로직
        // 실제로는 더 정교한 알고리즘 필요

        // 같은 메시지 연속 전송 체크
        String lastMessage = (String) userInfo.getAdditionalInfo().get("lastMessage");
        if (content.equals(lastMessage)) {
            Integer repeatCount = (Integer) userInfo.getAdditionalInfo().getOrDefault("repeatCount", 0);
            if (repeatCount >= 3) {
                return true; // 3번 연속 같은 메시지는 스팸으로 간주
            }
            userInfo.addAdditionalInfo("repeatCount", repeatCount + 1);
        } else {
            userInfo.addAdditionalInfo("repeatCount", 0);
        }

        userInfo.addAdditionalInfo("lastMessage", content);
        return false;
    }

    /**
     * 부적절한 내용 검증
     */
    private boolean containsInappropriateContent(String content) {
        // 욕설 필터링 로직
        // 실제로는 외부 필터링 서비스나 사전 기반 검증 구현
        String[] blockedWords = {"욕설1", "욕설2"}; // 예시

        String lowerContent = content.toLowerCase();
        for (String word : blockedWords) {
            if (lowerContent.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 사용자 접근 권한 검증
     */
    private boolean validateUserAccess(UserSessionInfo userInfo) {
        try {
            // 사용자 권한 검증 로직
            // 예: 밴 여부, 팀 소속 여부 등

            String teamId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            // 밴 여부 확인 (Redis에서 조회)
            // if (gameChatUserService.isUserBanned(teamId, userId)) {
            //     return false;
            // }

            // 팀 소속 여부 확인
            // if (!gameChatUserService.isTeamMember(teamId, userId)) {
            //     return false;
            // }

            return true;
        } catch (Exception e) {
            log.error("사용자 접근 권한 검증 실패 - userId: {}", userInfo.getUserId(), e);
            return false;
        }
    }

    /**
     * WebSocket 세션에서 팀 ID 추출
     */
    private String extractTeamId(WebSocketSession session) {
        // URL 파라미터나 헤더에서 teamId 추출
        // 예: /ws/game-chat?teamId=tigers
        Map<String, Object> attributes = session.getAttributes();
        String teamId = (String) attributes.get("teamId");

        if (teamId == null) {
            // URL에서 추출 시도
            String uri = session.getUri().toString();
            // 파라미터 파싱 로직
            teamId = parseTeamIdFromUri(uri);
        }

        return teamId != null ? teamId : "default";
    }

    /**
     * WebSocket 세션에서 사용자 ID 추출
     */
    private String extractUserId(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        String userId = (String) attributes.get("userId");

        if (userId == null) {
            // JWT 토큰에서 추출하거나 임시 ID 생성
            String sessionId = session.getId();
            userId = "user_" + (sessionId.length() >= 8 ? sessionId.substring(0, 8) : sessionId);
        }

        return userId;
    }

    /**
     * 사용자 이름 생성/추출
     */
    private String extractUserName(WebSocketSession session, String userId) {
        Map<String, Object> attributes = session.getAttributes();
        String userName = (String) attributes.get("userName");

        if (userName == null) {
            // 기본 사용자명 생성 (안전한 substring 처리)
            if (userId.length() >= 6) {
                userName = "User_" + userId.substring(userId.length() - 6);
            } else {
                userName = "User_" + userId;
            }
        }

        return userName;
    }

    /**
     * URI에서 팀 ID 파싱
     */
    private String parseTeamIdFromUri(String uri) {
        try {
            // 간단한 파라미터 파싱
            if (uri.contains("teamId=")) {
                String[] parts = uri.split("teamId=");
                if (parts.length > 1) {
                    String teamId = parts[1].split("&")[0];
                    return teamId;
                }
            }
        } catch (Exception e) {
            log.warn("URI에서 teamId 파싱 실패: {}", uri, e);
        }
        return null;
    }

    /**
     * 서버 식별자 반환
     */
    private String getServerId() {
        // 서버 식별자 (환경변수나 설정에서 가져오기)
        return System.getProperty("server.id", "server-1");
    }

    /**
     * 게임 채팅 통계 정보 조회
     */
    public Map<String, Object> getGameChatStats(String teamId) {
        Map<String, Object> stats = getConnectionStats();

        try {
            // 게임 채팅 특화 통계 추가
            stats.put("teamId", teamId);
            stats.put("isActive", gameChatRoomService.isChatRoomActive(teamId));
            stats.put("totalUsers", gameChatUserService.getConnectedUserCount(teamId));
            stats.put("currentTraffic", gameChatTrafficService.getCurrentTraffic(teamId));

            // 팀별 접속자 수
            Map<String, Long> teamUserCounts = new HashMap<>();
            sessionToUser.values().forEach(userInfo -> {
                String team = userInfo.getRoomId();
                teamUserCounts.merge(team, 1L, Long::sum);
            });
            stats.put("teamUserCounts", teamUserCounts);

        } catch (Exception e) {
            log.error("게임 채팅 통계 조회 실패 - teamId: {}", teamId, e);
        }

        return stats;
    }

    /**
     * 특정 팀 채팅방 구독 시작
     */
    public void subscribeToTeamChat(String teamId) {
        try {
            // Redis 구독 핸들러 정의
            RedisPubSubService.ChatMessageHandler handler = (roomId, message) -> {
                try {
                    log.debug("Redis 메시지 수신 - roomId: {}, message: {}", roomId, message);
                    
                    if (teamId.equals(roomId)) {
                        // 외부 서버에서 온 메시지만 브로드캐스트
                        if (isExternalMessage(message)) {
                            log.info("외부 메시지 브로드캐스트 - teamId: {}", teamId);
                            broadcastExternalMessage(roomId, message);
                        } else {
                            log.debug("내부 메시지 스킵 - teamId: {}", teamId);
                        }
                    }
                } catch (Exception e) {
                    log.error("Redis 메시지 처리 중 오류 - teamId: {}", teamId, e);
                }
            };

            // 실제 Redis 구독 시작
            redisPubSubService.subscribeToRoom(teamId, handler);
            log.info("✅ 팀 채팅방 Redis 구독 시작 - teamId: {}", teamId);

        } catch (Exception e) {
            log.error("❌ 팀 채팅방 구독 실패 - teamId: {}", teamId, e);
        }
    }

    /**
     * 특정 팀 채팅방 구독 해제
     */
    public void unsubscribeFromTeamChat(String teamId) {
        try {
            redisPubSubService.unsubscribeFromRoom(teamId);
            log.info("팀 채팅방 구독 해제 - teamId: {}", teamId);

        } catch (Exception e) {
            log.error("팀 채팅방 구독 해제 실패 - teamId: {}", teamId, e);
        }
    }
}
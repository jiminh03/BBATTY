package com.ssafy.chat.watch.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.UserSessionInfo;
import com.ssafy.chat.common.handler.BaseChatWebSocketHandler;
import com.ssafy.chat.common.service.RedisPubSubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 게임 채팅 WebSocket 핸들러 (단순화된 버전)
 */
@Component("gameChatWebSocketHandler")
@Slf4j
public class GameChatWebSocketHandler extends BaseChatWebSocketHandler {

    private final RedisPubSubService redisPubSubService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final String TRAFFIC_KEY_PREFIX = "chat:traffic:";
    private static final int TRAFFIC_SPIKE_THRESHOLD = 100; // 최근 3분간 100개 이상
    private static final int TRAFFIC_WINDOW_MINUTES = 3;

    @Autowired
    public GameChatWebSocketHandler(ObjectMapper objectMapper,
                                    RedisPubSubService redisPubSubService,
                                    RedisTemplate<String, Object> redisTemplate) {
        super(objectMapper);
        this.redisPubSubService = redisPubSubService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected UserSessionInfo createUserSessionInfo(WebSocketSession session) {
        try {
            // Query parameter에서 세션 토큰 추출
            URI uri = session.getUri();
            if (uri == null) {
                throw new IllegalArgumentException("WebSocket URI is null");
            }

            String query = uri.getQuery();
            Map<String, String> params = UriComponentsBuilder.fromUriString("?" + query)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();

            String sessionToken = params.get("token");
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                throw new IllegalArgumentException("Session token is required");
            }

            // Redis에서 세션 정보 조회
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
            
            if (sessionData.isEmpty()) {
                throw new IllegalArgumentException("Invalid or expired session token");
            }

            String userId = (String) sessionData.get("userId");
            String userName = (String) sessionData.get("userName");
            String userTeamId = (String) sessionData.get("userTeamId");
            String teamId = params.get("teamId"); // 채팅방 팀 ID는 query parameter에서

            if (userId == null || userName == null || userTeamId == null || teamId == null) {
                throw new IllegalArgumentException("Missing required session data");
            }

            // 세션에 필요한 정보 저장
            Map<String, Object> attributes = session.getAttributes();
            attributes.put("userId", userId);
            attributes.put("userName", userName);
            attributes.put("userTeamId", userTeamId);
            attributes.put("teamId", teamId);
            attributes.put("sessionToken", sessionToken);

            return new UserSessionInfo(userId, userName, teamId);

        } catch (Exception e) {
            log.error("세션 정보 생성 실패", e);
            throw new IllegalArgumentException("Failed to create user session info: " + e.getMessage());
        }
    }

    @Override
    protected boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId(); // 채팅방 팀 ID
            String userTeamId = (String) session.getAttributes().get("userTeamId"); // 사용자 응원팀
            
            log.info("채팅방 입장 검증 - 채팅방팀: {}, 사용자팀: {}", teamId, userTeamId);
            
            // 같은 팀인지 확인
            if (teamId != null && teamId.equals(userTeamId)) {
                log.info("팀 일치 - 입장 허용");
                return true;
            }
            
            log.warn("팀 불일치 - 채팅방팀: {}, 사용자팀: {}", teamId, userTeamId);
            return false;
            
        } catch (Exception e) {
            log.error("채팅방 입장 검증 실패 - userId: {}", userInfo.getUserId(), e);
            return false;
        }
    }

    @Override
    protected void handleConnectionManagement(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String userId = userInfo.getUserId();
            
            // 기존 연결이 있는지 확인
            Set<WebSocketSession> existingSessions = connectedUsers.get(userId);
            if (existingSessions != null && !existingSessions.isEmpty()) {
                log.info("게임 채팅 - 기존 연결 해제 - userId: {}, 기존 세션 수: {}", 
                        userId, existingSessions.size());
                
                // 기존 모든 세션 종료
                for (WebSocketSession existingSession : existingSessions) {
                    if (existingSession.isOpen()) {
                        try {
                            existingSession.close(CloseStatus.NORMAL.withReason("New connection established"));
                        } catch (Exception e) {
                            log.warn("기존 세션 종료 실패 - sessionId: {}", existingSession.getId(), e);
                        }
                    }
                }
                
                // 맵에서 제거
                existingSessions.clear();
                connectedUsers.remove(userId);
                
                // sessionToUser 맵에서도 제거
                sessionToUser.entrySet().removeIf(entry -> 
                    entry.getValue().getUserId().equals(userId));
            }
            
            log.info("게임 채팅 - 단일 세션 허용 - userId: {}", userId);
            
        } catch (Exception e) {
            log.error("게임 채팅 연결 관리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected void handleUserJoin(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            log.info("게임 채팅방 입장 완료 - teamId: {}, userId: {}", teamId, userId);

        } catch (Exception e) {
            log.error("게임 채팅방 입장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected void handleUserLeave(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            log.info("게임 채팅방 퇴장 완료 - teamId: {}, userId: {}", teamId, userId);

        } catch (Exception e) {
            log.error("게임 채팅방 퇴장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected Map<String, Object> handleDomainMessage(WebSocketSession session, UserSessionInfo userInfo, String content) {
        try {
            String teamId = userInfo.getRoomId();

            // 트래픽 카운트 증가 (급증 모니터링용)
            incrementTrafficCount(teamId);

            // 게임 채팅 메시지 생성 (익명 채팅)
            Map<String, Object> gameMessage = createGameChatMessage(userInfo, content);

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
        // 기본 검증
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        if (content.length() > 500) { // 최대 500자
            return false;
        }

        return true;
    }

    /**
     * 게임 채팅 메시지 생성 (익명 채팅)
     */
    private Map<String, Object> createGameChatMessage(UserSessionInfo userInfo, String content) {
        Map<String, Object> message = new HashMap<>();
        
        // 익명 채팅 - nickname 없음
        message.put("type", "message");
        message.put("roomId", userInfo.getRoomId());
        message.put("content", content);
        message.put("timestamp", java.time.LocalDateTime.now().toString());
        message.put("messageType", "CHAT");
        
        return message;
    }

    /**
     * 트래픽 카운트 증가 (급증 모니터링용)
     */
    private void incrementTrafficCount(String teamId) {
        try {
            String currentMinute = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            String trafficKey = TRAFFIC_KEY_PREFIX + teamId + ":" + currentMinute;
            
            // 현재 분의 메시지 수 증가
            redisTemplate.opsForValue().increment(trafficKey);
            redisTemplate.expire(trafficKey, TRAFFIC_WINDOW_MINUTES + 1, TimeUnit.MINUTES);
            
            // 급증 감지 (백그라운드에서 비동기 처리)
            checkTrafficSpike(teamId);
            
        } catch (Exception e) {
            log.warn("트래픽 카운트 증가 실패 - teamId: {}", teamId, e);
        }
    }

    /**
     * 트래픽 급증 감지
     */
    private void checkTrafficSpike(String teamId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            long totalMessages = 0;
            
            // 최근 N분간의 메시지 수 합계
            for (int i = 0; i < TRAFFIC_WINDOW_MINUTES; i++) {
                String minute = now.minusMinutes(i)
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
                String key = TRAFFIC_KEY_PREFIX + teamId + ":" + minute;
                
                Object count = redisTemplate.opsForValue().get(key);
                if (count != null) {
                    totalMessages += Long.parseLong(count.toString());
                }
            }
            
            // 임계값 초과 시 로그 출력
            if (totalMessages > TRAFFIC_SPIKE_THRESHOLD) {
                log.warn("채팅 트래픽 급증 감지 - teamId: {}, 최근 {}분간 메시지: {}개", 
                        teamId, TRAFFIC_WINDOW_MINUTES, totalMessages);
                
                // 필요하다면 여기서 알림이나 추가 처리 가능
                // 예: 관리자 알림, 레이트 리미팅 등
            }
            
        } catch (Exception e) {
            log.warn("트래픽 급증 감지 실패 - teamId: {}", teamId, e);
        }
    }
}
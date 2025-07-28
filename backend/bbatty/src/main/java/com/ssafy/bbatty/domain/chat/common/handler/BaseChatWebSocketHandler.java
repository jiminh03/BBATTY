package com.ssafy.bbatty.domain.chat.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.chat.game.service.GameChatUserService;
import com.ssafy.bbatty.domain.chat.game.service.GameChatUserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅 WebSocket 핸들러 기본 클래스
 */
@Slf4j
public abstract class BaseChatWebSocketHandler implements WebSocketHandler {

    protected final ObjectMapper objectMapper;
    protected final Map<String, WebSocketSession> connectedUsers = new ConcurrentHashMap<>();
    protected final Map<WebSocketSession, UserSessionInfo> sessionToUser = new ConcurrentHashMap<>();
    protected final GameChatUserService gameChatUserService;

    public BaseChatWebSocketHandler(ObjectMapper objectMapper, GameChatUserService gameChatUserService) {
        this.objectMapper = objectMapper;
        this.gameChatUserService = gameChatUserService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            UserSessionInfo userInfo = createUserSessionInfo(session);

            if (!canJoinChatRoom(session, userInfo)) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Access denied"));
                return;
            }

            // 세션 정보 저장
            connectedUsers.put(userInfo.getUserId(), session);
            sessionToUser.put(session, userInfo);

            // 도메인별 입장 처리
            handleUserJoin(session, userInfo);

            log.info("WebSocket 연결 성공 - userId: {}, sessionId: {}",
                    userInfo.getUserId(), session.getId());

        } catch (Exception e) {
            log.error("WebSocket 연결 처리 실패 - sessionId: {}", session.getId(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            UserSessionInfo userInfo = sessionToUser.get(session);
            if (userInfo != null) {
                String teamId = userInfo.getRoomId();
                // 도메인별 퇴장 처리
                handleUserLeave(session, userInfo);
                // 세션 정보 제거
                connectedUsers.remove(userInfo.getUserId());
                sessionToUser.remove(session);
                // 연결 해제시 리소스 정리 보장
                if (gameChatUserService.getConnectedUserCount(teamId) == 0) {
                    unsubscribeFromTeamChat(teamId);
                }

                log.info("WebSocket 연결 종료 - userId: {}, status: {}",
                        userInfo.getUserId(), status);
            }
        } catch (Exception e) {
            log.error("WebSocket 연결 종료 처리 실패 - sessionId: {}", session.getId(), e);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            UserSessionInfo userInfo = sessionToUser.get(session);
            if (userInfo == null) {
                log.warn("세션 정보 없음 - sessionId: {}", session.getId());
                return;
            }

            if (message instanceof TextMessage) {
                String messageContent = ((TextMessage) message).getPayload();

                // 메시지 검증
                if (!isValidMessage(messageContent, userInfo)) {
                    log.warn("유효하지 않은 메시지 - userId: {}", userInfo.getUserId());
                    return;
                }

                // 도메인별 메시지 처리
                Map<String, Object> processedMessage = handleDomainMessage(session, userInfo, messageContent);

                if (processedMessage != null) {
                    // 같은 방의 다른 사용자들에게 브로드캐스트
                    broadcastToRoom(userInfo.getRoomId(), processedMessage);
                }
            }

        } catch (Exception e) {
            log.error("메시지 처리 실패 - sessionId: {}", session.getId(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 전송 오류 - sessionId: {}", session.getId(), exception);

        UserSessionInfo userInfo = sessionToUser.get(session);
        if (userInfo != null) {
            handleUserLeave(session, userInfo);
        }

        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 사용자 세션 정보 생성 (하위 클래스에서 구현)
     */
    protected abstract UserSessionInfo createUserSessionInfo(WebSocketSession session);

    /**
     * 채팅방 입장 가능 여부 확인 (하위 클래스에서 구현)
     */
    protected abstract boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo);

    /**
     * 사용자 입장 처리 (하위 클래스에서 구현)
     */
    protected abstract void handleUserJoin(WebSocketSession session, UserSessionInfo userInfo);

    /**
     * 사용자 퇴장 처리 (하위 클래스에서 구현)
     */
    protected abstract void handleUserLeave(WebSocketSession session, UserSessionInfo userInfo);

    /**
     * 도메인별 메시지 처리 (하위 클래스에서 구현)
     */
    protected abstract Map<String, Object> handleDomainMessage(WebSocketSession session, UserSessionInfo userInfo, String content);

    /**
     * 메시지 유효성 검증 (하위 클래스에서 구현)
     */
    protected abstract boolean isValidMessage(String content, UserSessionInfo userInfo);

    /**
     * 기본 채팅 메시지 생성
     */
    protected Map<String, Object> createChatMessage(UserSessionInfo userInfo, String content) {
        Map<String, Object> message = new ConcurrentHashMap<>();
        message.put("type", "message");
        message.put("userId", userInfo.getUserId());
        message.put("userName", userInfo.getUserName());
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }

    /**
     * 특정 사용자에게 메시지 전송
     */
    protected void sendMessageToUser(WebSocketSession session, Map<String, Object> message) {
        try {
            if (session.isOpen()) {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            }
        } catch (Exception e) {
            log.error("사용자 메시지 전송 실패 - sessionId: {}", session.getId(), e);
        }
    }

    /**
     * 방의 모든 사용자에게 브로드캐스트
     */
    protected void broadcastToRoom(String roomId, Map<String, Object> message) {
        sessionToUser.entrySet().stream()
                .filter(entry -> roomId.equals(entry.getValue().getRoomId()))
                .forEach(entry -> sendMessageToUser(entry.getKey(), message));
    }

    /**
     * 연결 통계 정보 반환
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalConnections", connectedUsers.size());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    /**
     * 사용자 세션 정보 클래스
     */
    public static class UserSessionInfo {
        private final String userId;
        private final String userName;
        private final String roomId;
        private final Map<String, Object> additionalInfo = new ConcurrentHashMap<>();

        public UserSessionInfo(String userId, String userName, String roomId) {
            this.userId = userId;
            this.userName = userName;
            this.roomId = roomId;
        }

        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getRoomId() { return roomId; }
        public Map<String, Object> getAdditionalInfo() { return additionalInfo; }

        public void addAdditionalInfo(String key, Object value) {
            additionalInfo.put(key, value);
        }
    }
    protected abstract void unsubscribeFromTeamChat(String teamId);

}
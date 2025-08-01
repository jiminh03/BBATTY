package com.ssafy.chat.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.UserSessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅 WebSocket 핸들러 기본 클래스
 */
@Slf4j
public abstract class BaseChatWebSocketHandler implements WebSocketHandler {

    protected final ObjectMapper objectMapper;
    protected final Map<String, Set<WebSocketSession>> connectedUsers = new ConcurrentHashMap<>();
    protected final Map<WebSocketSession, UserSessionInfo> sessionToUser = new ConcurrentHashMap<>();

    public BaseChatWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            UserSessionInfo userInfo = createUserSessionInfo(session);

            if (!canJoinChatRoom(session, userInfo)) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Access denied"));
                return;
            }

            // 도메인별 연결 관리 처리
            handleConnectionManagement(session, userInfo);

            // 세션 정보 저장
            connectedUsers.computeIfAbsent(userInfo.getUserId(), k -> ConcurrentHashMap.newKeySet()).add(session);
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
                Set<WebSocketSession> userSessions = connectedUsers.get(userInfo.getUserId());
                if (userSessions != null) {
                    userSessions.remove(session);
                    if (userSessions.isEmpty()) {
                        connectedUsers.remove(userInfo.getUserId());
                    }
                }
                sessionToUser.remove(session);

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
                    // 같은 방의 다른 사용자들에게 브로드캐스트 (발신자 제외)
                    broadcastToRoom(userInfo.getRoomId(), processedMessage, session);
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
     * 연결 관리 처리 (하위 클래스에서 구현)
     * - Match: 여러 세션 허용
     * - Watch: 단일 세션만 허용 (기존 연결 해제)
     */
    protected abstract void handleConnectionManagement(WebSocketSession session, UserSessionInfo userInfo);

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
        message.put("nickname", userInfo.getUserName());  // nickname으로 변경
        message.put("content", content);
        message.put("timestamp", java.time.LocalDateTime.now().toString()); // String으로 변경
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
     * 방의 모든 사용자에게 브로드캐스트 (발신자 제외)
     */
    protected void broadcastToRoom(String roomId, Map<String, Object> message, WebSocketSession excludeSession) {
        sessionToUser.entrySet().stream()
                .filter(entry -> roomId.equals(entry.getValue().getRoomId()))
                .filter(entry -> !entry.getKey().equals(excludeSession))
                .forEach(entry -> sendMessageToUser(entry.getKey(), message));
    }

    /**
     * 연결 통계 정보 반환
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        int totalSessions = connectedUsers.values().stream()
                .mapToInt(Set::size)
                .sum();
        stats.put("totalUsers", connectedUsers.size());
        stats.put("totalSessions", totalSessions);
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    /**
     * 테스트용: 연결된 사용자 세션 정보 반환
     */
    public Set<WebSocketSession> getConnectedUserSessions(String userId) {
        return connectedUsers.get(userId);
    }

    /**
     * 테스트용: 세션-사용자 매핑 정보 반환
     */
    public UserSessionInfo getUserSessionInfo(WebSocketSession session) {
        return sessionToUser.get(session);
    }

}
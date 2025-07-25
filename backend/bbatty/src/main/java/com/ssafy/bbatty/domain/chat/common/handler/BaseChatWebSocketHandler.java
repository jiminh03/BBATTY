package com.ssafy.bbatty.domain.chat.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅 WebSocket 핸들러의 기본 클래스
 * 공통 기능을 제공
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseChatWebSocketHandler extends TextWebSocketHandler {

    protected final ObjectMapper objectMapper;

    // 접속 중인 모든 사용자 관리 Map
    // key: 세션 ID, value: WebSocketSession
    protected final Map<String, WebSocketSession> connectedUsers = new ConcurrentHashMap<>();

    // 세션 ID -> 사용자 정보 매핑
    protected final Map<String, UserSessionInfo> sessionToUser = new ConcurrentHashMap<>();

    /**
     * 연결 수립 시 호출
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        try {
            String sessionId = session.getId();

            // 도메인별 사용자 정보 생성
            UserSessionInfo userInfo = createUserSessionInfo(session);

            // 채팅방 입장 가능 여부 확인
            if (!canJoinChatRoom(session, userInfo)) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("채팅방 입장 불가"));
                return;
            }

            // 사용자 추가
            connectedUsers.put(sessionId, session);
            sessionToUser.put(sessionId, userInfo);

            log.info("사용자 입장: {} (세션: {}, 방: {}) - 총 {}명",
                    userInfo.getUserName(), sessionId, userInfo.getRoomId(), connectedUsers.size());

            // 도메인별 입장 처리
            handleUserJoin(session, userInfo);

            // 입장한 사용자에게 환영 메시지
            sendMessageToUser(session, createSystemMessage("채팅방에 입장했습니다!"));

            // 다른 사용자들에게 입장 알림
            String joinMessage = userInfo.getUserName() + "님이 입장했습니다. (현재 " + connectedUsers.size() + "명)";
            broadcastMessage(createSystemMessage(joinMessage), sessionId);

        } catch (Exception e) {
            log.error("연결 수립 중 오류 발생", e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 메시지 수신 시 호출
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            String sessionId = session.getId();
            UserSessionInfo userInfo = sessionToUser.get(sessionId);

            if (userInfo == null) {
                log.warn("사용자 정보를 찾을 수 없음: {}", sessionId);
                return;
            }

            String content = message.getPayload();
            log.debug("채팅 메시지 - {}: {}", userInfo.getUserName(), content);

            // 메시지 유효성 검사
            if (!isValidMessage(content, userInfo)) {
                sendMessageToUser(session, createErrorMessage("유효하지 않은 메시지입니다."));
                return;
            }

            // 도메인별 메시지 처리
            Map<String, Object> processedMessage = handleDomainMessage(session, userInfo, content);

            if (processedMessage != null) {
                // 메시지 브로드캐스트
                broadcastMessage(processedMessage, null);

                // 사용자 활동 시간 업데이트
                updateUserActivity(userInfo);
            }

        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
            sendMessageToUser(session, createErrorMessage("메시지 처리에 실패했습니다."));
        }
    }

    /**
     * 연결 종료 시 호출
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String sessionId = session.getId();
        UserSessionInfo userInfo = sessionToUser.get(sessionId);

        // 사용자 제거
        connectedUsers.remove(sessionId);
        sessionToUser.remove(sessionId);

        if (userInfo != null) {
            log.info("사용자 퇴장: {} (세션: {}, 방: {}) - 총 {}명",
                    userInfo.getUserName(), sessionId, userInfo.getRoomId(), connectedUsers.size());

            // 도메인별 퇴장 처리
            handleUserLeave(session, userInfo);

            // 퇴장 알림
            String leaveMessage = userInfo.getUserName() + "님이 퇴장했습니다. (현재 " + connectedUsers.size() + "명)";
            broadcastMessage(createSystemMessage(leaveMessage), null);
        }
    }

    /**
     * 전송 오류 시 호출
     */
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        String sessionId = session.getId();
        UserSessionInfo userInfo = sessionToUser.get(sessionId);

        log.error("사용자 {} 연결 오류", userInfo != null ? userInfo.getUserName() : sessionId, exception);

        // 오류 발생한 사용자 정리
        connectedUsers.remove(sessionId);
        sessionToUser.remove(sessionId);

        if (userInfo != null) {
            handleUserLeave(session, userInfo);
        }

        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 모든 사용자에게 메시지 브로드캐스트
     */
    protected void broadcastMessage(Map<String, Object> message, String excludeSessionId) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);

            // 연결 끊어진 세션 제거하면서 메시지 전송
            connectedUsers.entrySet().removeIf(entry -> {
                String sessionId = entry.getKey();
                WebSocketSession session = entry.getValue();

                // 제외할 세션이면 스킵
                if (excludeSessionId != null && excludeSessionId.equals(sessionId)) {
                    return false;
                }

                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                        return false; // 세션 유지
                    } else {
                        log.warn("닫힌 세션 제거: {}", sessionId);
                        sessionToUser.remove(sessionId);
                        return true; // 세션 제거
                    }
                } catch (Exception e) {
                    log.error("메시지 전송 실패 - 세션: {}", sessionId, e);
                    sessionToUser.remove(sessionId);
                    return true; // 실패한 세션 제거
                }
            });

            log.debug("메시지 브로드캐스트 완료 - 전송 대상: {}명", connectedUsers.size());
        } catch (Exception e) {
            log.error("브로드캐스트 실패", e);
        }
    }

    /**
     * 특정 사용자에게만 메시지 전송
     */
    protected void sendMessageToUser(WebSocketSession session, Map<String, Object> message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(messageJson));
        } catch (Exception e) {
            log.error("개별 메시지 전송 실패", e);
        }
    }

    /**
     * 기본 채팅 메시지 생성
     */
    protected Map<String, Object> createChatMessage(UserSessionInfo userInfo, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "chat");
        message.put("userName", userInfo.getUserName());
        message.put("userId", userInfo.getUserId());
        message.put("roomId", userInfo.getRoomId());
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        message.put("userCount", connectedUsers.size());
        return message;
    }

    /**
     * 시스템 메시지 생성
     */
    protected Map<String, Object> createSystemMessage(String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "system");
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        message.put("userCount", connectedUsers.size());
        return message;
    }

    /**
     * 오류 메시지 생성
     */
    protected Map<String, Object> createErrorMessage(String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "error");
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }

    /**
     * 사용자 활동 시간 업데이트
     */
    private void updateUserActivity(UserSessionInfo userInfo) {
        userInfo.setLastActivity(LocalDateTime.now());
    }

    /**
     * 현재 연결 상태 정보 조회
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", connectedUsers.size());
        stats.put("connectedUsers", sessionToUser.values().stream()
                .map(UserSessionInfo::getUserName)
                .toList());
        stats.put("handlerType", getClass().getSimpleName());
        return stats;
    }

    // ========== 추상 메서드 (하위 클래스에서 구현) ==========

    /**
     * 도메인별 사용자 세션 정보 생성
     */
    protected abstract UserSessionInfo createUserSessionInfo(WebSocketSession session);

    /**
     * 채팅방 입장 가능 여부 확인
     */
    protected abstract boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo);

    /**
     * 도메인별 사용자 입장 처리
     */
    protected abstract void handleUserJoin(WebSocketSession session, UserSessionInfo userInfo);

    /**
     * 도메인별 사용자 퇴장 처리
     */
    protected abstract void handleUserLeave(WebSocketSession session, UserSessionInfo userInfo);

    /**
     * 도메인별 메시지 처리
     */
    protected abstract Map<String, Object> handleDomainMessage(WebSocketSession session, UserSessionInfo userInfo, String content);

    /**
     * 메시지 유효성 검사
     */
    protected abstract boolean isValidMessage(String content, UserSessionInfo userInfo);

    /**
     * 사용자 세션 정보 클래스
     */
    public static class UserSessionInfo {
        private String userId;
        private String userName;
        private String roomId;
        private LocalDateTime joinTime;
        private LocalDateTime lastActivity;
        private Map<String, Object> additionalInfo;

        public UserSessionInfo(String userId, String userName, String roomId) {
            this.userId = userId;
            this.userName = userName;
            this.roomId = roomId;
            this.joinTime = LocalDateTime.now();
            this.lastActivity = LocalDateTime.now();
            this.additionalInfo = new HashMap<>();
        }

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }

        public LocalDateTime getJoinTime() { return joinTime; }
        public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }

        public LocalDateTime getLastActivity() { return lastActivity; }
        public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

        public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
        public void setAdditionalInfo(Map<String, Object> additionalInfo) { this.additionalInfo = additionalInfo; }

        public void addAdditionalInfo(String key, Object value) {
            this.additionalInfo.put(key, value);
        }
    }
}
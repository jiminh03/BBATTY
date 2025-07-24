package com.ssafy.bbatty.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler{
    private final ObjectMapper objectMapper; //의존성 주입

    // 접속 중인 모든 사용자 관리 Map
    // key : 세션 Id, value : WebSocketSession
    private final Map<String, WebSocketSession> connectedUsers = new ConcurrentHashMap<>();

    // 세션 ID -> 사용자 정보 매핑 (누락된 변수 추가)
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String userName = "User_" + sessionId.substring(0, 8); // 앞 8글자 사용자명으로

        // add user
        connectedUsers.put(sessionId, session);
        sessionToUser.put(sessionId, userName);

        log.info("사용자 입장: {} (세션: {}) - 총 {}명", userName, sessionId, connectedUsers.size());

        // 입장한 사용자에게 환영 메시지
        sendMessageToUser(session, createSystemMessage("채팅방에 입장! 환영!"));

        // 다른 모든 사용자들에게 입장 알림
        String joinMessage = userName + "님이 입장했습니다. (현재 " + connectedUsers.size() + "명)";
        broadcastMessage(createSystemMessage(joinMessage), sessionId);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String sessionId = session.getId();
        String userName = sessionToUser.get(sessionId);
        String content = message.getPayload();

        log.info("채팅 메시지 - {}: {}", userName, content);

        // 채팅 메시지 설정
        Map<String, Object> chatMessage = createChatMessage(userName, content);
        broadcastMessage(chatMessage, null); // 모든 사용자에게
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String userName = sessionToUser.get(sessionId);

        // 사용자 제거 (수정: sessionId로 제거해야 함)
        connectedUsers.remove(sessionId);
        sessionToUser.remove(sessionId);

        log.info("사용자 퇴장: {} (세션: {}) - 총 {}명", userName, sessionId, connectedUsers.size());

        // 퇴장 알림
        if (userName != null){
            String leaveMessage = userName + "님이 퇴장했습니다. (현재 " + connectedUsers.size() + "명)";
            broadcastMessage(createSystemMessage(leaveMessage), null);
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        String sessionId = session.getId();
        String userName = sessionToUser.get(sessionId);

        log.error("사용자 {} 연결 오류", userName, exception);

        // 오류 발생한 사용자 정리
        connectedUsers.remove(sessionId);
        sessionToUser.remove(sessionId);

        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 모든 사용자에게 메시지 브로드캐스트
     * @param message 전송할 메시지
     * @param excludeSessionId 제외할 세션 ID (null이면 모든 사용자에게 전송)
     */
    private void broadcastMessage(Map<String, Object> message, String excludeSessionId){
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);

            // 연결 끊어진 세션 제거
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
                        sessionToUser.remove(sessionId); // 오타 수정: sessionID -> sessionId
                        return true; // 세션 제거
                    }
                } catch (Exception e) {
                    log.error("메시지 전송 실패 - 세션: {}", sessionId, e);
                    sessionToUser.remove(sessionId);
                    return true; //실패한 세션 제거
                }
            });

            log.debug("메시지 브로드캐스트 완료 - 전송 대상 : {}명", connectedUsers.size());
        } catch (Exception e){
            log.error("브로드캐스트 실패", e);
        }
    }

    /**
     * 특정 사용자에게만 메시지 전송
     */
    private void sendMessageToUser(WebSocketSession session, Map<String, Object> message){
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(messageJson));
        } catch (Exception e) {
            log.error("개별 메시지 전송 실패", e);
        }
    }

    /**
     * 채팅 메시지 생성
     */
    private Map<String, Object> createChatMessage(String userName, String content){
        Map<String, Object> message = new HashMap<>();
        message.put("type", "chat");
        message.put("userName", userName);
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        message.put("userCount", connectedUsers.size());
        return message;
    }

    /**
     * 시스템 메시지 생성 (입/퇴장 알림)
     */
    private Map<String, Object> createSystemMessage(String content){
        Map<String, Object> message = new HashMap<>();
        message.put("type", "system");
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        message.put("userCount", connectedUsers.size());
        return message;
    }

    /**
     * 현재 연결 상태 정보 조회 (디버깅/모니터링용)
     */
    public Map<String, Object> getConnectionStats(){
        Map<String, Object> stats = new HashMap<>(); // 변수명 수정
        stats.put("totalUsers", connectedUsers.size());
        stats.put("connectedUsers", sessionToUser.values());
        return stats;
    }
}
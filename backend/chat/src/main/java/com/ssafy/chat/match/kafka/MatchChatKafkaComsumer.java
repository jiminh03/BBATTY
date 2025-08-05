package com.ssafy.chat.match.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 매칭 채팅 kafka Consumer
 * 동적으로 생성되는 매칭 채팅방별 토픽을 구독하여 메시지를 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchChatKafkaComsumer {
    private final ObjectMapper objectMapper;
    //활성화된 매칭 채팅방별 websocket 세션 관리
    private final Map<String, Set<WebSocketSession>> matchChatSessions = new ConcurrentHashMap<>();
    /**
     * 매칭 채팅 메시지 수신 처리
     * 토픽 패턴 : match-chat-{matchId}
     */
    @KafkaListener(
            topicPattern = "match-chat-.*",
            groupId = "match-chat-service"
    )
    public void handleMatchChatMessage(
            @Payload String messageJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            // 토픽에서 matchId 추출
            String matchId = extractMatchIdFromTopic(topic);
            log.debug("kafka 메시지 수신 - topic: {}, matchId: {}", topic, matchId);
            // json은 Map으로 파싱
            Map<String, Object> messageData = objectMapper.readValue(messageJson, Map.class);
            broadcastToMatchChatRoom(matchId, messageData);
        } catch (Exception e) {
            log.error("Kafka 메시지 처리 실패 - topic: {}", topic, e);
        }
    }

    /**
     * 토픽명에서 matchId 추출
     */
    private String extractMatchIdFromTopic(String topic) {
        return topic.replace("match-chat-", "");
    }

    /**
     * 매칭 채팅방의 모든 세션에 메시지 브로드캐스트
     */
    private void broadcastToMatchChatRoom(String matchId, Map<String, Object> messageData) {
        Set<WebSocketSession> sessions = matchChatSessions.get(matchId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("활성화된 세션이 없음 - matchId: {}", matchId);
            return;
        }
        try {
            String messageJson = objectMapper.writeValueAsString(messageData);
            TextMessage textMessage = new TextMessage(messageJson);
            // 동시성 처리를 위해 세션 복사
            Set<WebSocketSession> sessionSet = Set.copyOf(sessions);
            for (WebSocketSession session : sessionSet) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                        log.debug("메시지 전송 성공 - matchId: {}, sessionId: {}", matchId, session.getId());
                    } else {
                        // 닫힌 세션 제거
                        sessions.remove(session);
                        log.debug("닫힌 세션 제거 - matchId: {}, sessionId: {}", matchId, session.getId());
                    }
                } catch (Exception e){
                    log.error("개별 세션 메시지 전송 실패 - matchId: {}, sessionId: {}", matchId, session.getId(), e);
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            log.error("메시지 브로드캐스트 실패 - matchId: {}", matchId, e);
        }
    }
    /**
     * 매칭 채팅방에 websocket 세션 추가
     */
    public void addSessionToMatchChatRoom(String matchId, WebSocketSession session) {
        matchChatSessions.computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("세션 추가 - matchId: {}, sessionId: {}, 총 세션 수: {}", matchId, session.getId(), matchChatSessions.size());
    }
    /**
     * 매칭 채팅방에서 websocket 세션 제거
     */
    public void removeSessioniFromMatchRoom(String matchId, WebSocketSession session) {
        Set<WebSocketSession> sessions = matchChatSessions.get(matchId);
        if (sessions != null) {
            sessions.remove(session);
            log.debug("세션 제거 - matchId: {}, sessionId: {}, 남은 세션 수: {}", matchId, session.getId(), sessions.size());
            // 세션이 모두 없어지면 맵에서 제거
            if (sessions.isEmpty()) {
                matchChatSessions.remove(matchId);
                log.debug("빈 체팅방 제거 - matchId: {}", matchId);
            }
        }
    }
    /**
     * 현재 활성화된 매칭 채팅방 수 반환
     */
    public int getActiveMatchRoomCount(){
        return matchChatSessions.size();
    }
    /**
     * 특정 방의 활성 세션 수 반환
     */
    public int getActiveSessionCount(String matchId){
        Set<WebSocketSession> sessions = matchChatSessions.get(matchId);
        return sessions != null ? sessions.size() : 0;
    }

}

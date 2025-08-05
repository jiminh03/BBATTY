package com.ssafy.chat.match.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 매칭 채팅 kafka Consumer
 * 동적으로 생성되는 매칭 채팅방별 토픽을 구독하여 메시지를 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchChatKafkaComsumer {
    private final ObjectMapper objectMapper;
    private final ConsumerFactory<String, String> consumerFactory;
    //활성화된 매칭 채팅방별 websocket 세션 관리
    private final Map<String, Set<WebSocketSession>> matchChatSessions = new ConcurrentHashMap<>();
    private static final String TOPIC_PREFIX = "match-chat-";
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
            log.info("🔥 실시간 Kafka 메시지 수신 - topic: {}, matchId: {}", topic, matchId);
            // json은 Map으로 파싱
            Map<String, Object> messageData = objectMapper.readValue(messageJson, Map.class);
            log.info("🔥 메시지 파싱 완료 - messageType: {}, content: {}", messageData.get("messageType"), messageData.get("content"));
            broadcastToMatchChatRoom(matchId, messageData);
            log.info("🔥 브로드캐스트 완료 - matchId: {}", matchId);
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
        log.info("🔥 세션 추가 - matchId: {}, sessionId: {}, 해당 방 세션 수: {}, 전체 활성 방: {}", 
                matchId, session.getId(), matchChatSessions.get(matchId).size(), matchChatSessions.keySet());
        // 새 세션에게 최근 메시지 히스토리 전송
        sendRecentmessagesToSession(matchId, session);
    }

    private void sendRecentmessagesToSession(String matchId, WebSocketSession session) {
        try {
            List<Map<String, Object>> recentMessages = getRecentMessages(matchId, 50);
            log.info("🔥 히스토리 조회 - matchId: {}, sessionId: {}, 조회된 메시지 수: {}", matchId, session.getId(), recentMessages.size());
            
            if (!recentMessages.isEmpty()) {
                // 처음 몇 개 메시지의 roomId 확인
                for (int i = 0; i < Math.min(3, recentMessages.size()); i++) {
                    Map<String, Object> msg = recentMessages.get(i);
                    log.info("🔥 히스토리 메시지 샘플 {} - roomId: {}, content: {}", i+1, msg.get("roomId"), msg.get("content"));
                }
                
                for (Map<String, Object> recentMessage : recentMessages) {
                    if (session.isOpen()) {
                        String messageJson = objectMapper.writeValueAsString(recentMessage);
                        session.sendMessage(new TextMessage(messageJson));
                    } else {
                        log.warn("세션이 닫혀 있어 히스토리 전송 중단 - matchId: {}, sessionId: {}", matchId, session.getId());
                        break;
                    }
                }
            }
        } catch (Exception e){
            log.error("새 세션 히스토리 전송 실패 - matchId:{}, sessionId: {}", matchId, session.getId(), e);
        }
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
    /**
     * 매칭 채팅방의 최근 메시지 히스토리 조회
     * @param matchId 매칭 Id
     * @param limit 조회할 최대 메시지 수 (0 이하면 기본값 50 사용)
     * @return 최근 메시지 목록 (실제 존재하는 메시지 수 만큼 반환)
     */
    public List<Map<String, Object>> getRecentMessages(String matchId, int limit){
        // limit 검증 및 기본값 설정
        if (limit <= 0){
            limit = 50;
        }
        
        String topicName = TOPIC_PREFIX + matchId;
        log.info("🔥 Kafka 토픽 조회 시작 - matchId: {}, topic: {}, limit: {}", matchId, topicName, limit);
        
        List<Map<String, Object>> messages = new ArrayList<>();
        
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            // 단일 파티션 가정 (파티션 0)
            TopicPartition partition = new TopicPartition(topicName, 0);
            
            // 파티션 할당
            consumer.assign(Collections.singletonList(partition));
            
            // 파티션 끝으로 이동
            consumer.seekToEnd(Collections.singletonList(partition));
            
            // 현재 offset 확인
            long endOffset = consumer.position(partition);
            if (endOffset == 0) {
                log.debug("토픽에 메시지가 없음 - topic: {}", topicName);
                return messages;
            }
            
            // limit만큼 뒤로 이동하여 읽기 시작점 설정
            long startOffset = Math.max(0, endOffset - limit);
            consumer.seek(partition, startOffset);
            
            log.debug("offset 설정 - topic: {}, start: {}, end: {}", topicName, startOffset, endOffset);
            
            // 메시지 읽기
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            
            for (ConsumerRecord<String, String> record : records) {
                try {
                    Map<String, Object> messageData = objectMapper.readValue(record.value(), Map.class);
                    // timestamp 추가 (record의 timestamp 사용)
                    messageData.put("kafkaTimestamp", record.timestamp());
                    messages.add(messageData);
                } catch (Exception e) {
                    log.error("메시지 파싱 실패 - offset: {}, value: {}", record.offset(), record.value(), e);
                }
            }
            
            // timestamp 기준으로 정렬 (최신순)
            messages.sort((m1, m2) -> {
                Long ts1 = (Long) m1.getOrDefault("kafkaTimestamp", 0L);
                Long ts2 = (Long) m2.getOrDefault("kafkaTimestamp", 0L);
                return ts2.compareTo(ts1);
            });
            
            log.info("🔥 Kafka 조회 완료 - topic: {}, 조회된 메시지 수: {}", topicName, messages.size());
            
            // 조회된 메시지들의 roomId 확인
            for (int i = 0; i < Math.min(3, messages.size()); i++) {
                Map<String, Object> msg = messages.get(i);
                log.info("🔥 Kafka 메시지 샘플 {} - roomId: {}, content: {}", i+1, msg.get("roomId"), msg.get("content"));
            }
            
        } catch (Exception e) {
            log.error("최근 메시지 조회 실패 - topic: {}", topicName, e);
        }
        
        return messages;
    }

    /**
     * 매칭 채팅방의 특정 시점 이전 메시지 히스토리 조회 (페이징용)
     * @param matchId 매칭 ID
     * @param lastMessageTimestamp 마지막으로 받은 메시지의 timestamp (이 시점 이전 메시지들을 조회)
     * @param limit 조회할 최대 메시지 수 (0 이하면 기본값 50 사용)
     * @return 해당 시점 이전의 메시지 목록
     */
    public List<Map<String, Object>> getRecentMessages(String matchId, long lastMessageTimestamp, int limit) {
        // limit 검증 및 기본값 설정
        if (limit <= 0) {
            limit = 50;
        }
        
        String topicName = TOPIC_PREFIX + matchId;
        log.debug("특정 시점 이전 메시지 조회 시작 - topic: {}, beforeTimestamp: {}, limit: {}", 
                topicName, lastMessageTimestamp, limit);
        
        List<Map<String, Object>> messages = new ArrayList<>();
        
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            // 단일 파티션 가정 (파티션 0)
            TopicPartition partition = new TopicPartition(topicName, 0);
            
            // 파티션 할당
            consumer.assign(Collections.singletonList(partition));
            
            // timestamp 기반으로 offset 찾기
            Map<TopicPartition, Long> timestampToSearch = Collections.singletonMap(partition, lastMessageTimestamp);
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndTimestamp> offsetsForTimes = 
                    consumer.offsetsForTimes(timestampToSearch);
            
            long targetOffset = 0;
            if (offsetsForTimes.get(partition) != null) {
                targetOffset = offsetsForTimes.get(partition).offset();
            }
            
            // targetOffset 이전의 메시지들을 읽기 위해 더 이전 offset으로 이동
            long startOffset = Math.max(0, targetOffset - limit);
            consumer.seek(partition, startOffset);
            
            log.debug("offset 설정 - topic: {}, start: {}, target: {}", topicName, startOffset, targetOffset);
            
            // 메시지 읽기
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            
            for (ConsumerRecord<String, String> record : records) {
                // lastMessageTimestamp 이전의 메시지만 필터링
                if (record.timestamp() < lastMessageTimestamp) {
                    try {
                        Map<String, Object> messageData = objectMapper.readValue(record.value(), Map.class);
                        // timestamp 추가 (record의 timestamp 사용)
                        messageData.put("kafkaTimestamp", record.timestamp());
                        messages.add(messageData);
                    } catch (Exception e) {
                        log.error("메시지 파싱 실패 - offset: {}, value: {}", record.offset(), record.value(), e);
                    }
                }
            }
            
            // timestamp 기준으로 정렬 (최신순)
            messages.sort((m1, m2) -> {
                Long ts1 = (Long) m1.getOrDefault("kafkaTimestamp", 0L);
                Long ts2 = (Long) m2.getOrDefault("kafkaTimestamp", 0L);
                return ts2.compareTo(ts1);
            });
            
            // limit 적용
            if (messages.size() > limit) {
                messages = messages.subList(0, limit);
            }
            
            log.debug("특정 시점 이전 메시지 조회 완료 - topic: {}, 조회된 메시지 수: {}", topicName, messages.size());
            
        } catch (Exception e) {
            log.error("특정 시점 이전 메시지 조회 실패 - topic: {}", topicName, e);
        }
        
        return messages;
    }



}










package com.ssafy.chat.match.service;

import com.ssafy.chat.common.util.KSTTimeUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.match.dto.MatchChatMessage;
import com.ssafy.chat.match.kafka.MatchChatKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 매칭 채팅 서비스 구현체
 * Kafka Producer/Consumer와 연동하여 매칭 채팅 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchChatServiceImpl implements MatchChatService {
    
    private final MatchChatKafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;
    private final ConsumerFactory<String, String> consumerFactory;
    
    private final Map<String, Set<WebSocketSession>> matchChatSessions = new ConcurrentHashMap<>();
    private static final String TOPIC_PREFIX = "match-chat-";
    
    @Override
    public void addSessionToMatchRoom(String matchId, WebSocketSession session) {
        log.debug("매칭 채팅방에 세션 추가 - matchId: {}, sessionId: {}", matchId, session.getId());
        matchChatSessions.computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("🔥 세션 추가 - matchId: {}, sessionId: {}, 해당 방 세션 수: {}, 전체 활성 방: {}", 
                matchId, session.getId(), matchChatSessions.get(matchId).size(), matchChatSessions.keySet());
        sendRecentMessagesToSession(matchId, session);
    }
    
    @Override
    public void removeSessionFromMatchRoom(String matchId, WebSocketSession session) {
        log.debug("매칭 채팅방에서 세션 제거 - matchId: {}, sessionId: {}", matchId, session.getId());
        Set<WebSocketSession> sessions = matchChatSessions.get(matchId);
        if (sessions != null) {
            sessions.remove(session);
            log.debug("세션 제거 - matchId: {}, sessionId: {}, 남은 세션 수: {}", matchId, session.getId(), sessions.size());
            if (sessions.isEmpty()) {
                matchChatSessions.remove(matchId);
                log.debug("빈 채팅방 제거 - matchId: {}", matchId);
            }
        }
    }
    
    @Override
    public void sendChatMessage(String matchId, MatchChatMessage message) {
        log.debug("채팅 메시지 발송 - matchId: {}, userId: {}", matchId, message.getUserId());
        kafkaProducer.sendChatMessage(matchId, message);
    }
    
    @Override
    public void sendUserJoinEvent(String matchId, Long userId, String nickname) {
        log.debug("사용자 입장 이벤트 발송 - matchId: {}, userId: {}", matchId, userId);
        kafkaProducer.sendUserJoinEvent(matchId, userId, nickname);
    }
    
    @Override
    public void sendUserLeaveEvent(String matchId, Long userId, String nickname) {
        log.debug("사용자 퇴장 이벤트 발송 - matchId: {}, userId: {}", matchId, userId);
        kafkaProducer.sendUserLeaveEvent(matchId, userId, nickname);
    }
    
    @Override
    public List<Map<String, Object>> getRecentMessages(String matchId, int limit) {
        log.debug("최근 메시지 조회 - matchId: {}, limit: {}", matchId, limit);
        return getRecentMessagesFromKafka(matchId, limit);
    }
    
    @Override
    public List<Map<String, Object>> getRecentMessages(String matchId, long lastMessageTimestamp, int limit) {
        log.debug("특정 시점 이전 메시지 조회 - matchId: {}, beforeTimestamp: {}, limit: {}", 
                matchId, lastMessageTimestamp, limit);
        return getRecentMessagesFromKafka(matchId, lastMessageTimestamp, limit);
    }
    
    @Override
    public int getActiveMatchRoomCount() {
        return matchChatSessions.size();
    }
    
    @Override
    public int getActiveSessionCount(String matchId) {
        Set<WebSocketSession> sessions = matchChatSessions.get(matchId);
        return sessions != null ? sessions.size() : 0;
    }
    
    @Override
    public void handleKafkaMessage(String matchId, Map<String, Object> messageData) {
        broadcastToMatchChatRoom(matchId, messageData);
    }
    
    @Override
    public void sendSystemMessageToRoom(String matchId, String message) {
        Map<String, Object> systemMessage = Map.of(
                "messageType", "SYSTEM",
                "roomId", matchId,
                "timestamp", KSTTimeUtil.nowAsString(),
                "content", message,
                "isSystemMessage", true
        );
        broadcastToMatchChatRoom(matchId, systemMessage);
        log.info("🔔 시스템 메시지 전송 - matchId: {}, message: {}", matchId, message);
    }
    
    @Override
    public void forceCloseRoomSessions(String matchId) {
        Set<WebSocketSession> sessions = matchChatSessions.get(matchId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("강제 종료할 세션 없음 - matchId: {}", matchId);
            return;
        }
        
        log.info("🔒 매칭 채팅방 세션 강제 종료 시작 - matchId: {}, 세션 수: {}", matchId, sessions.size());
        
        // 복사본 생성 (ConcurrentModificationException 방지)
        Set<WebSocketSession> sessionCopy = Set.copyOf(sessions);
        int closedCount = 0;
        
        for (WebSocketSession session : sessionCopy) {
            try {
                if (session.isOpen()) {
                    session.close();
                    closedCount++;
                }
            } catch (Exception e) {
                log.error("세션 강제 종료 실패 - sessionId: {}", session.getId(), e);
            }
        }
        
        // 메모리에서 해당 채팅방 세션 정보 완전 제거
        matchChatSessions.remove(matchId);
        
        log.info("✅ 매칭 채팅방 세션 강제 종료 완료 - matchId: {}, 종료된 세션 수: {}", matchId, closedCount);
    }
    
    private void broadcastToMatchChatRoom(String matchId, Map<String, Object> messageData) {
        Set<WebSocketSession> sessions = matchChatSessions.get(matchId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("활성화된 세션이 없음 - matchId: {}", matchId);
            return;
        }
        try {
            String messageJson = objectMapper.writeValueAsString(messageData);
            TextMessage textMessage = new TextMessage(messageJson);
            Set<WebSocketSession> sessionSet = Set.copyOf(sessions);
            for (WebSocketSession session : sessionSet) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                        log.debug("메시지 전송 성공 - matchId: {}, sessionId: {}", matchId, session.getId());
                    } else {
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
    
    private void sendRecentMessagesToSession(String matchId, WebSocketSession session) {
        try {
            List<Map<String, Object>> recentMessages = getRecentMessages(matchId, 50);
            log.info("🔥 히스토리 조회 - matchId: {}, sessionId: {}, 조회된 메시지 수: {}", matchId, session.getId(), recentMessages.size());
            
            if (!recentMessages.isEmpty()) {
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
    
    private List<Map<String, Object>> getRecentMessagesFromKafka(String matchId, int limit) {
        if (limit <= 0) limit = 50;
        
        String topicName = TOPIC_PREFIX + matchId;
        log.info("🔥 Kafka 토픽 조회 시작 - matchId: {}, topic: {}, limit: {}", matchId, topicName, limit);
        
        List<Map<String, Object>> messages = new ArrayList<>();
        
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            TopicPartition partition = new TopicPartition(topicName, 0);
            consumer.assign(Collections.singletonList(partition));
            consumer.seekToEnd(Collections.singletonList(partition));
            
            long endOffset = consumer.position(partition);
            if (endOffset == 0) {
                log.debug("토픽에 메시지가 없음 - topic: {}", topicName);
                return messages;
            }
            
            long startOffset = Math.max(0, endOffset - limit);
            consumer.seek(partition, startOffset);
            log.debug("offset 설정 - topic: {}, start: {}, end: {}", topicName, startOffset, endOffset);
            
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            
            for (ConsumerRecord<String, String> record : records) {
                try {
                    Map<String, Object> messageData = objectMapper.readValue(record.value(), Map.class);
                    messageData.put("kafkaTimestamp", record.timestamp());
                    messages.add(messageData);
                } catch (Exception e) {
                    log.error("메시지 파싱 실패 - offset: {}, value: {}", record.offset(), record.value(), e);
                }
            }
            
            messages.sort((m1, m2) -> {
                Long ts1 = (Long) m1.getOrDefault("kafkaTimestamp", 0L);
                Long ts2 = (Long) m2.getOrDefault("kafkaTimestamp", 0L);
                return ts2.compareTo(ts1);
            });
            
            log.info("🔥 Kafka 조회 완료 - topic: {}, 조회된 메시지 수: {}", topicName, messages.size());
            
        } catch (Exception e) {
            log.error("최근 메시지 조회 실패 - topic: {}", topicName, e);
        }
        
        return messages;
    }
    
    private List<Map<String, Object>> getRecentMessagesFromKafka(String matchId, long lastMessageTimestamp, int limit) {
        if (limit <= 0) limit = 50;
        
        String topicName = TOPIC_PREFIX + matchId;
        log.debug("특정 시점 이전 메시지 조회 시작 - topic: {}, beforeTimestamp: {}, limit: {}", 
                topicName, lastMessageTimestamp, limit);
        
        List<Map<String, Object>> messages = new ArrayList<>();
        
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            TopicPartition partition = new TopicPartition(topicName, 0);
            consumer.assign(Collections.singletonList(partition));
            
            Map<TopicPartition, Long> timestampToSearch = Collections.singletonMap(partition, lastMessageTimestamp);
            Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndTimestamp> offsetsForTimes = 
                    consumer.offsetsForTimes(timestampToSearch);
            
            long targetOffset = 0;
            if (offsetsForTimes.get(partition) != null) {
                targetOffset = offsetsForTimes.get(partition).offset();
            }
            
            long startOffset = Math.max(0, targetOffset - limit);
            consumer.seek(partition, startOffset);
            log.debug("offset 설정 - topic: {}, start: {}, target: {}", topicName, startOffset, targetOffset);
            
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            
            for (ConsumerRecord<String, String> record : records) {
                if (record.timestamp() < lastMessageTimestamp) {
                    try {
                        Map<String, Object> messageData = objectMapper.readValue(record.value(), Map.class);
                        messageData.put("kafkaTimestamp", record.timestamp());
                        messages.add(messageData);
                    } catch (Exception e) {
                        log.error("메시지 파싱 실패 - offset: {}, value: {}", record.offset(), record.value(), e);
                    }
                }
            }
            
            messages.sort((m1, m2) -> {
                Long ts1 = (Long) m1.getOrDefault("kafkaTimestamp", 0L);
                Long ts2 = (Long) m2.getOrDefault("kafkaTimestamp", 0L);
                return ts2.compareTo(ts1);
            });
            
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
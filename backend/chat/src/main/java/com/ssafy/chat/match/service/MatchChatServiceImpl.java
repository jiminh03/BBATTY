package com.ssafy.chat.match.service;

import com.ssafy.chat.common.util.KSTTimeUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.match.dto.MatchChatMessage;
import com.ssafy.chat.match.kafka.MatchChatKafkaProducer;
import com.ssafy.chat.common.service.DistributedSessionManagerService;
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
    private final DistributedSessionManagerService distributedSessionManager;
    
    // 로컬 세션 캐시 제거 - 분산 세션 매니저 사용
    private static final String TOPIC_PREFIX = "match-chat-";
    
    @Override
    public void addSessionToMatchRoom(String matchId, WebSocketSession session) {
        log.debug("매칭 채팅방에 분산 세션 추가 - matchId: {}, sessionId: {}", matchId, session.getId());
        
        // 분산 세션 매니저는 이미 ChatWebSocketHandler에서 등록됨
        // 여기서는 히스토리만 전송
        sendRecentMessagesToSession(matchId, session);
        
        int sessionCount = distributedSessionManager.getActiveSessionCount(matchId);
        log.info("🔥 매치 채팅 세션 추가 완료 - matchId: {}, sessionId: {}, 해당 방 세션 수: {}", 
                matchId, session.getId(), sessionCount);
    }
    
    @Override
    public void removeSessionFromMatchRoom(String matchId, WebSocketSession session) {
        log.debug("매칭 채팅방에서 분산 세션 제거 - matchId: {}, sessionId: {}", matchId, session.getId());
        
        // 분산 세션 매니저는 ChatWebSocketHandler에서 해제됨
        int remainingCount = distributedSessionManager.getActiveSessionCount(matchId);
        log.info("매치 채팅 세션 제거 완료 - matchId: {}, sessionId: {}, 남은 세션 수: {}", 
                matchId, session.getId(), remainingCount);
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
        return distributedSessionManager.getTotalActiveRoomCount();
    }
    
    @Override
    public int getActiveSessionCount(String matchId) {
        return distributedSessionManager.getActiveSessionCount(matchId);
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
        try {
            int sessionCount = distributedSessionManager.getActiveSessionCount(matchId);
            if (sessionCount == 0) {
                log.debug("강제 종료할 세션 없음 - matchId: {}", matchId);
                return;
            }
            
            log.info("🔒 매칭 채팅방 분산 세션 강제 종료 시작 - matchId: {}, 세션 수: {}", matchId, sessionCount);
            
            // 분산 세션 매니저를 통해 해당 방의 모든 세션 정리
            // 실제 WebSocket 세션 종료는 분산 세션 매니저에서 처리
            int cleanedCount = distributedSessionManager.cleanupRoomSessions(matchId);
            
            log.info("✅ 매칭 채팅방 분산 세션 강제 종료 완료 - matchId: {}, 정리된 세션 수: {}", matchId, cleanedCount);
            
        } catch (Exception e) {
            log.error("매칭 채팅방 분산 세션 강제 종료 실패 - matchId: {}", matchId, e);
        }
    }
    
    private void broadcastToMatchChatRoom(String matchId, Map<String, Object> messageData) {
        try {
            String messageJson = objectMapper.writeValueAsString(messageData);
            
            // 🚀 분산 세션 매니저를 통한 브로드캐스트 (모든 인스턴스에 전파)
            distributedSessionManager.broadcastToRoom(matchId, messageJson, null);
            
            log.debug("분산 매치 채팅 브로드캐스트 완료 - matchId: {}", matchId);
            
        } catch (Exception e) {
            log.error("분산 매치 채팅 브로드캐스트 실패 - matchId: {}", matchId, e);
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
            log.info("🔥 토픽 offset 정보 - topic: {}, endOffset: {}", topicName, endOffset);
            if (endOffset == 0) {
                log.info("🔥 토픽에 메시지가 없음 - topic: {}", topicName);
                return messages;
            }
            
            long startOffset = Math.max(0, endOffset - limit);
            consumer.seek(partition, startOffset);
            log.info("🔥 offset 설정 - topic: {}, start: {}, end: {}", topicName, startOffset, endOffset);
            
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            log.info("🔥 poll 결과 - topic: {}, records count: {}", topicName, records.count());
            
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
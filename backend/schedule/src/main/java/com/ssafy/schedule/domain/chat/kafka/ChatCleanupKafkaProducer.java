package com.ssafy.schedule.domain.chat.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 채팅방 정리 이벤트를 Kafka로 전송하는 Producer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatCleanupKafkaProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String MATCH_CHAT_CLEANUP_TOPIC = "match-chat-cleanup";
    
    /**
     * 첫 번째 경고 이벤트 전송 (23:50)
     */
    public void sendWarning1Event() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        sendCleanupEvent("warning_1", today);
    }
    
    /**
     * 두 번째 경고 이벤트 전송 (23:54)
     */
    public void sendWarning2Event() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        sendCleanupEvent("warning_2", today);
    }
    
    /**
     * 실제 정리 이벤트 전송 (23:55)
     */
    public void sendCleanupEvent() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        sendCleanupEvent("cleanup", today);
    }
    
    /**
     * 정리 이벤트를 Kafka로 전송
     */
    private void sendCleanupEvent(String action, LocalDate targetDate) {
        try {
            Map<String, Object> cleanupRequest = new HashMap<>();
            cleanupRequest.put("action", action);
            cleanupRequest.put("date", targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            String messageJson = objectMapper.writeValueAsString(cleanupRequest);
            String key = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            log.info("채팅방 정리 이벤트 Kafka 전송: action={}, date={}", action, targetDate);
            
            kafkaTemplate.send(MATCH_CHAT_CLEANUP_TOPIC, key, messageJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("채팅방 정리 이벤트 Kafka 전송 실패: action={}, date={}, error={}", 
                                    action, targetDate, ex.getMessage(), ex);
                        } else {
                            log.info("✅ 채팅방 정리 이벤트 Kafka 전송 성공: action={}, date={}, offset={}", 
                                    action, targetDate, result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("채팅방 정리 이벤트 직렬화 실패: action={}, date={}", action, targetDate, e);
        }
    }
}
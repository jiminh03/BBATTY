package com.ssafy.bbatty.domain.chat.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 채팅 인증/인가 결과를 Kafka로 전송하는 Producer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAuthKafkaProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CHAT_AUTH_TOPIC = "chat-auth-result";
    
    /**
     * 채팅 인증 결과를 Kafka로 전송
     */
    public void sendAuthResult(String requestId, Map<String, Object> authResult) {
        try {
            String messageJson = objectMapper.writeValueAsString(authResult);
            log.debug("채팅 인증 결과 Kafka 전송: requestId={}", requestId);
            
            kafkaTemplate.send(CHAT_AUTH_TOPIC, requestId, messageJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("채팅 인증 결과 Kafka 전송 실패: requestId={}", requestId, ex);
                        } else {
                            log.debug("채팅 인증 결과 Kafka 전송 성공: requestId={}, offset={}", 
                                    requestId, result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("채팅 인증 결과 직렬화 실패: requestId={}", requestId, e);
        }
    }
}
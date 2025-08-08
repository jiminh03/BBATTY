package com.ssafy.chat.match.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.match.service.MatchChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 매칭 채팅 kafka Consumer
 * 동적으로 생성되는 매칭 채팅방별 토픽을 구독하여 메시지를 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchChatKafkaComsumer {
    private final MatchChatService matchChatService;
    private final ObjectMapper objectMapper;
    /**
     * 매칭 채팅 메시지 수신 처리
     * 토픽 패턴 : match-chat-{matchId}
     */
    @KafkaListener(
            topicPattern = "match-chat-.*",
            groupId = "match-chat-consumer-group"
    )
    public void handleMatchChatMessage(
            @Payload String messageJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            String matchId = extractMatchIdFromTopic(topic);
            log.info("🔥 실시간 Kafka 메시지 수신 - topic: {}, matchId: {}", topic, matchId);
            Map<String, Object> messageData = objectMapper.readValue(messageJson, Map.class);
            log.info("🔥 메시지 파싱 완료 - messageType: {}, content: {}", messageData.get("messageType"), messageData.get("content"));
            
            matchChatService.handleKafkaMessage(matchId, messageData);
            log.info("🔥 서비스로 위임 완료 - matchId: {}", matchId);
        } catch (Exception e) {
            log.error("Kafka 메시지 처리 실패 - topic: {}", topic, e);
        }
    }

    private String extractMatchIdFromTopic(String topic) {
        return topic.replace("match-chat-", "");
    }



}










package com.ssafy.chat.match.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.dto.MatchChatMessage;
import com.ssafy.chat.match.service.MatchChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchChatKafkaComsumer {
    private final MatchChatService matchChatService;
    private final ObjectMapper objectMapper;

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
            log.debug("🔍 수신된 원본 JSON: {}", messageJson);

            // 🔥 기존 MatchChatMessage DTO 활용으로 직렬화 문제 해결
            MatchChatMessage message = objectMapper.readValue(messageJson, MatchChatMessage.class);

            log.info("🔥 메시지 파싱 완료 - messageType: {}, content: {}, userId: {}",
                    message.getMessageType(), message.getContent(), message.getUserId());

            // Map으로 변환해서 기존 서비스 로직 유지
            Map<String, Object> messageData = objectMapper.convertValue(message, Map.class);

            matchChatService.handleKafkaMessage(matchId, messageData);
            log.info("🔥 서비스로 위임 완료 - matchId: {}", matchId);

        } catch (JsonProcessingException e) {
            log.error("🚨 JSON 파싱 실패 - topic: {}, json: {}", topic, messageJson, e);
            throw new ApiException(ErrorCode.JSON_PARSING_FAILED);
        } catch (Exception e) {
            log.error("🚨 Kafka 메시지 처리 실패 - topic: {}", topic, e);
        }
    }

    private String extractMatchIdFromTopic(String topic) {
        String matchId = topic.replace("match-chat-", "");
        log.debug("토픽에서 matchId 추출: {} -> {}", topic, matchId);
        return matchId;
    }
}

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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchChatKafkaComsumer {
    private final MatchChatService matchChatService;
    private final ObjectMapper objectMapper;

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @KafkaListener(
            topicPattern = "match-chat-match_.*",
            groupId = "match-chat-consumer-group"
    )
    public void handleMatchChatMessage(
            @Payload String messageJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            String matchId = extractMatchIdFromTopic(topic);
            log.info("🔥 실시간 Kafka 메시지 수신 - topic: {}, matchId: {}", topic, matchId);
            log.debug("🔍 수신된 원본 JSON: {}", messageJson);

            // 먼저 Map으로 파싱해서 중첩 구조 처리
            Map<String, Object> rawData = objectMapper.readValue(messageJson, Map.class);


            // roomCreateInfo 중첩 데이터를 플랫 구조로 변환
            Map<String, Object> flattenedData = flattenRoomCreateInfo(rawData);
            
            // 플랫 데이터를 MatchChatMessage로 변환
            MatchChatMessage message = objectMapper.convertValue(flattenedData, MatchChatMessage.class);

            log.info("🔥 메시지 파싱 완료 - messageType: {}, content: {}, userId: {}",
                    message.getMessageType(), message.getContent(), message.getUserId());

            // 기존 서비스 로직에 플랫 데이터 전달
            matchChatService.handleKafkaMessage(matchId, flattenedData);
            log.info("🔥 서비스로 위임 완료 - matchId: {}", matchId);

        } catch (JsonProcessingException e) {
            log.error("🚨 JSON 파싱 실패 - topic: {}, json: {}", topic, messageJson, e);
            throw new ApiException(ErrorCode.JSON_PARSING_FAILED);
        } catch (Exception e) {
            log.error("🚨 Kafka 메시지 처리 실패 - topic: {}, 재시도 중...", topic, e);
            throw e; // 재시도를 위해 예외 재발생
        }
    }

    /**
     * roomCreateInfo 중첩 구조를 플랫 구조로 변환
     */
    private Map<String, Object> flattenRoomCreateInfo(Map<String, Object> rawData) {
        Map<String, Object> flattened = new HashMap<>(rawData);
        
        // roomCreateInfo가 있으면 내용을 추출해서 플랫 구조로 변환
        Object roomCreateInfoObj = rawData.get("roomCreateInfo");
        if (roomCreateInfoObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> roomCreateInfo = (Map<String, Object>) roomCreateInfoObj;
            
            // roomCreateInfo의 내용을 최상위 레벨로 복사
            if (roomCreateInfo.get("title") != null) {
                flattened.put("title", roomCreateInfo.get("title"));
            }
            if (roomCreateInfo.get("genderRestriction") != null) {
                flattened.put("genderRestriction", roomCreateInfo.get("genderRestriction"));
            }
            if (roomCreateInfo.get("ageRestriction") != null) {
                flattened.put("ageRestriction", roomCreateInfo.get("ageRestriction"));
            }
            if (roomCreateInfo.get("roomType") != null) {
                flattened.put("roomType", roomCreateInfo.get("roomType"));
            }
            
            log.debug("🔄 roomCreateInfo 플랫 변환 완료 - title: {}, genderRestriction: {}", 
                    roomCreateInfo.get("title"), roomCreateInfo.get("genderRestriction"));
        }
        
        return flattened;
    }
    
    private String extractMatchIdFromTopic(String topic) {
        // match-chat-request 같은 시스템 토픽은 제외
        if (topic.equals("match-chat-request") || topic.equals("match-chat-response")) {
            log.debug("시스템 토픽 감지: {}", topic);
            return topic; // 원본 토픽명 그대로 반환
        }
        
        String matchId = topic.replace("match-chat-", "");
        log.debug("토픽에서 matchId 추출: {} -> {}", topic, matchId);
        return matchId;
    }
}

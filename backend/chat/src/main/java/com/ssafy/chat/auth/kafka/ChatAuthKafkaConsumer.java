package com.ssafy.chat.auth.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.match.service.MatchChatAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.ssafy.chat.common.util.RedisUtil;

import java.time.Duration;
import java.util.Map;

/**
 * 채팅 인증 결과를 수신하는 Kafka Consumer
 * 기존 MatchChatAuthService의 세션 관리 기능을 활용
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAuthKafkaConsumer {
    
    private final ObjectMapper objectMapper;
    private final RedisUtil redisUtil;
    
    private static final String AUTH_RESULT_KEY_PREFIX = "chat_auth_result:";
    private static final Duration AUTH_RESULT_EXPIRE_TIME = Duration.ofMinutes(10);
    
    @KafkaListener(topics = "chat-auth-result", groupId = "chat-service-auth")
    public void handleAuthResult(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String requestId) {
        
        try {
            log.debug("채팅 인증 결과 수신: requestId={}", requestId);
            
            // JSON 메시지 파싱
            Map<String, Object> authResult = objectMapper.readValue(message, Map.class);
            
            // Redis에 인증 결과 저장 (클라이언트가 폴링으로 확인할 수 있도록)
            String resultKey = AUTH_RESULT_KEY_PREFIX + requestId;
            redisUtil.setValue(resultKey, authResult, AUTH_RESULT_EXPIRE_TIME);
            
            boolean success = (Boolean) authResult.get("success");
            
            if (success) {
                log.info("채팅 인증 성공 처리: requestId={}", requestId);
            } else {
                String errorMessage = (String) authResult.get("errorMessage");
                log.warn("채팅 인증 실패 처리: requestId={}, error={}", requestId, errorMessage);
            }
            
        } catch (Exception e) {
            log.error("채팅 인증 결과 처리 실패: requestId={}", requestId, e);
        }
    }
}
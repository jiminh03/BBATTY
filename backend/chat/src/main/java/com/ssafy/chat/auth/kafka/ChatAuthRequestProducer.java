package com.ssafy.chat.auth.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 채팅 인증 요청을 bbatty 서버로 전송하는 Kafka Producer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAuthRequestProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CHAT_AUTH_REQUEST_TOPIC = "chat-auth-request";
    
    /**
     * bbatty 서버에 채팅 인증 요청 전송
     */
    public String sendAuthRequest(String jwtToken, String chatType, String action, 
                                 Long gameId, Map<String, Object> roomInfo, String nickname) {

        String requestId = UUID.randomUUID().toString();

        try {
            // 인증 요청 메시지 생성
            Map<String, Object> authRequest = new HashMap<>();
            authRequest.put("requestId", requestId);
            authRequest.put("jwtToken", jwtToken);
            authRequest.put("chatType", chatType);
            authRequest.put("action", action);
            authRequest.put("gameId", gameId);
            authRequest.put("roomInfo", roomInfo); // 채팅방 정보 포함
            authRequest.put("timestamp", System.currentTimeMillis());
            
            // nickname은 MATCH 채팅에만 필요
            if ("MATCH".equals(chatType) && nickname != null) {
                authRequest.put("nickname", nickname);
            }
            
            String message = objectMapper.writeValueAsString(authRequest);
            
            // Kafka로 메시지 전송
            kafkaTemplate.send(CHAT_AUTH_REQUEST_TOPIC, requestId, message);
            
            log.info("채팅 인증 요청 전송: requestId={}, chatType={}, action={}, gameId={}", 
                    requestId, chatType, action, gameId);
            
            return requestId; // 결과 폴링용 요청 ID 반환
            
        } catch (Exception e) {
            log.error("채팅 인증 요청 전송 실패: requestId={}", requestId, e);
            return null;
        }
    }
}
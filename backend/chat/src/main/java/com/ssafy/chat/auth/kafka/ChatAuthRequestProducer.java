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
    
    private static final String MATCH_CHAT_REQUEST_TOPIC = "match-chat-request";
    private static final String WATCH_CHAT_REQUEST_TOPIC = "watch-chat-request";
    
    /**
     * bbatty 서버에 채팅 인증 요청 전송
     */
    public String sendAuthRequest(String jwtToken, String chatType, String action, 
                                 Long gameId, Map<String, Object> roomInfo, String nickname) {

        String requestId = UUID.randomUUID().toString();
        String topic = "MATCH".equals(chatType) ? MATCH_CHAT_REQUEST_TOPIC : WATCH_CHAT_REQUEST_TOPIC;

        try {
            // 인증 요청 메시지 생성
            Map<String, Object> authRequest = new HashMap<>();
            authRequest.put("requestId", requestId);
            authRequest.put("jwtToken", jwtToken);
            authRequest.put("chatType", chatType);
            authRequest.put("action", action);
            authRequest.put("gameId", gameId);
            authRequest.put("roomInfo", roomInfo);
            authRequest.put("timestamp", System.currentTimeMillis());
            
            // nickname은 MATCH 채팅에만 필요
            if ("MATCH".equals(chatType) && nickname != null) {
                authRequest.put("nickname", nickname);
            }
            
            String message = objectMapper.writeValueAsString(authRequest);
            
            // 채팅 타입에 따라 다른 topic으로 전송
            kafkaTemplate.send(topic, requestId, message);
            
            log.info("채팅 인증 요청 전송: requestId={}, chatType={}, action={}, gameId={}, topic={}", 
                    requestId, chatType, action, gameId, topic);
            
            return requestId;
            
        } catch (Exception e) {
            log.error("채팅 인증 요청 전송 실패: requestId={}", requestId, e);
            return null;
        }
    }
    
    /**
     * Match 채팅방 생성 요청 전송
     */
    public String sendMatchChatCreateRequest(String jwtToken, Long gameId, 
                                           Map<String, Object> roomCreateInfo, String nickname) {
        String requestId = UUID.randomUUID().toString();

        try {
            Map<String, Object> createRequest = new HashMap<>();
            createRequest.put("requestId", requestId);
            createRequest.put("jwtToken", jwtToken);
            createRequest.put("chatType", "MATCH");
            createRequest.put("action", "CREATE");
            createRequest.put("gameId", gameId);
            createRequest.put("roomCreateInfo", roomCreateInfo); // 방 생성 조건들
            createRequest.put("nickname", nickname);
            createRequest.put("timestamp", System.currentTimeMillis());
            
            String message = objectMapper.writeValueAsString(createRequest);
            kafkaTemplate.send(MATCH_CHAT_REQUEST_TOPIC, requestId, message);
            
            log.info("Match 채팅방 생성 요청 전송: requestId={}, gameId={}", requestId, gameId);
            
            return requestId;
            
        } catch (Exception e) {
            log.error("Match 채팅방 생성 요청 전송 실패: requestId={}", requestId, e);
            return null;
        }
    }
}
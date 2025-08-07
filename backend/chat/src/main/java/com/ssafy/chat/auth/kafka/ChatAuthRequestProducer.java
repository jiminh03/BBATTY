package com.ssafy.chat.auth.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
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
     * Match 채팅방 입장 요청 전송
     */
    public String sendMatchChatJoinRequest(String jwtToken, String matchId,
                                           Map<String, Object> roomInfo, String nickname) {

        String requestId = UUID.randomUUID().toString();

        try {
            // 인증 요청 메시지 생성 (JWT 토큰은 Header로 분리)
            Map<String, Object> authRequest = new HashMap<>();
            authRequest.put("requestId", requestId);
            authRequest.put("chatType", "MATCH");
            authRequest.put("action", "JOIN");
            authRequest.put("gameId", Long.parseLong(matchId));  // matchId → gameId로 통일, Long 타입
            authRequest.put("roomId", matchId);  // 실제 채팅방 ID로 사용
            authRequest.put("roomInfo", roomInfo);
            authRequest.put("nickname", nickname);
            authRequest.put("timestamp", System.currentTimeMillis());

            String message = objectMapper.writeValueAsString(authRequest);

            // Header에 JWT 토큰 포함해서 전송
            var messageWithHeaders = MessageBuilder.withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, MATCH_CHAT_REQUEST_TOPIC)
                    .setHeader(KafkaHeaders.KEY, requestId)
                    .setHeader("Authorization", "Bearer " + jwtToken)
                    .build();

            kafkaTemplate.send(messageWithHeaders);

            log.info("Match 채팅방 입장 요청 전송: requestId={}, gameId={}", requestId, matchId);

            return requestId;

        } catch (Exception e) {
            log.error("Match 채팅방 입장 요청 전송 실패: requestId={}", requestId, e);
            return null;
        }
    }

    /**
     * Watch 채팅방 입장 요청 전송
     */
    public String sendWatchChatJoinRequest(String jwtToken, Long gameId,
                                           Map<String, Object> roomInfo) {

        String requestId = UUID.randomUUID().toString();

        try {
            // 인증 요청 메시지 생성 (JWT 토큰은 Header로 분리)
            Map<String, Object> authRequest = new HashMap<>();
            authRequest.put("requestId", requestId);
            authRequest.put("chatType", "WATCH");
            authRequest.put("action", "JOIN");
            authRequest.put("gameId", gameId);  // 일관된 gameId 사용
            authRequest.put("roomInfo", roomInfo);
            authRequest.put("timestamp", System.currentTimeMillis());

            String message = objectMapper.writeValueAsString(authRequest);

            // Header에 JWT 토큰 포함해서 전송
            var messageWithHeaders = MessageBuilder.withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, WATCH_CHAT_REQUEST_TOPIC)
                    .setHeader(KafkaHeaders.KEY, requestId)
                    .setHeader("Authorization", "Bearer " + jwtToken)
                    .build();

            kafkaTemplate.send(messageWithHeaders);

            log.info("Watch 채팅방 입장 요청 전송: requestId={}, gameId={}", requestId, gameId);

            return requestId;

        } catch (Exception e) {
            log.error("Watch 채팅방 입장 요청 전송 실패: requestId={}", requestId, e);
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
            createRequest.put("chatType", "MATCH");
            createRequest.put("action", "CREATE");
            createRequest.put("gameId", gameId);  // 일관된 gameId 사용
            createRequest.put("roomCreateInfo", roomCreateInfo); // 방 생성 조건들
            createRequest.put("nickname", nickname);
            createRequest.put("timestamp", System.currentTimeMillis());

            String message = objectMapper.writeValueAsString(createRequest);

            // Header에 JWT 토큰 포함해서 전송
            var messageWithHeaders = MessageBuilder.withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, MATCH_CHAT_REQUEST_TOPIC)
                    .setHeader(KafkaHeaders.KEY, requestId)
                    .setHeader("Authorization", "Bearer " + jwtToken)
                    .build();

            kafkaTemplate.send(messageWithHeaders);

            log.info("Match 채팅방 생성 요청 전송: requestId={}, gameId={}", requestId, gameId);

            return requestId;

        } catch (Exception e) {
            log.error("Match 채팅방 생성 요청 전송 실패: requestId={}", requestId, e);
            return null;
        }
    }
}

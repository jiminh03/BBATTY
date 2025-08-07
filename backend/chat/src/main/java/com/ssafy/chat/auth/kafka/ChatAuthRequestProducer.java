package com.ssafy.chat.auth.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
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
            // ✅ matchId에서 gameId 추출 (NumberFormatException 해결)
            Long gameId = extractGameIdFromMatchId(matchId);

            // 인증 요청 메시지 생성 (JWT 토큰은 Header로 분리)
            Map<String, Object> authRequest = new HashMap<>();
            authRequest.put("requestId", requestId);
            authRequest.put("chatType", "MATCH");
            authRequest.put("action", "JOIN");
            authRequest.put("gameId", gameId);           // 추출된 gameId (Long 타입)
            authRequest.put("roomId", matchId);          // 전체 matchId는 roomId로 사용
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

            log.info("Match 채팅방 입장 요청 전송: requestId={}, matchId={}, gameId={}",
                    requestId, matchId, gameId);

            return requestId;

        } catch (Exception e) {
            log.error("Match 채팅방 입장 요청 전송 실패: requestId={}, matchId={}",
                    requestId, matchId, e);
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
            authRequest.put("gameId", gameId);          // 일관된 gameId 사용
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
            log.error("Watch 채팅방 입장 요청 전송 실패: requestId={}, gameId={}",
                    requestId, gameId, e);
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
            createRequest.put("gameId", gameId);         // 일관된 gameId 사용
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
            log.error("Match 채팅방 생성 요청 전송 실패: requestId={}, gameId={}",
                    requestId, gameId, e);
            return null;
        }
    }

    /**
     * matchId에서 gameId 추출
     *
     * @param matchId 매칭 채팅방 ID (예: "match_1_67271f38")
     * @return gameId Long 타입으로 추출된 게임 ID (예: 1L)
     * @throws ApiException matchId 형식이 올바르지 않은 경우
     */
    private Long extractGameIdFromMatchId(String matchId) {
        try {
            if (matchId == null || matchId.trim().isEmpty()) {
                throw new ApiException(ErrorCode.MATCH_ID_MISSING);
            }

            // "match_1_67271f38" → ["match", "1", "67271f38"]로 분할
            String[] parts = matchId.split("_");

            if (parts.length < 2) {
                throw new ApiException(ErrorCode.MATCH_ID_FORMAT_INVALID);
            }

            if (!"match".equals(parts[0])) {
                throw new ApiException(ErrorCode.MATCH_ID_PREFIX_INVALID);
            }

            // 두 번째 부분이 gameId
            return Long.parseLong(parts[1]);

        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.MATCH_ID_GAME_ID_INVALID);
        }
    }
}

package com.ssafy.bbatty.domain.chat.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.service.ChatAuthService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Status;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Watch 채팅 관련 요청을 처리하는 Kafka Consumer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WatchChatRequestConsumer {
    
    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final ChatAuthService chatAuthService;
    private final ChatAuthKafkaProducer chatAuthKafkaProducer;
    
    @KafkaListener(topics = "watch-chat-request", groupId = "bbatty-watch-chat-group")
    public void handleWatchChatRequest(@Payload String message,
                                     @Header("Authorization") String authHeader) {
        String requestId = null;
        
        try {
            JsonNode requestNode = objectMapper.readTree(message);
            requestId = requestNode.get("requestId").asText();
            
            // Header에서 JWT 토큰 추출
            String jwtToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwtToken = authHeader.substring(7);
            } else {
                throw new ApiException(ErrorCode.UNAUTHORIZED);
            }
            
            String action = requestNode.get("action").asText();
            Long gameId = requestNode.get("gameId").asLong();
            
            log.info("Watch 채팅 요청 수신: requestId={}, action={}, gameId={}", 
                    requestId, action, gameId);
            
            // JWT 토큰에서 사용자 정보 추출
            var claims = jwtProvider.getClaims(jwtToken);
            Long userId = Long.valueOf(claims.getSubject()); // subject에서 userId 추출
            Long userTeamId = claims.get("teamId", Long.class);
            String userGender = claims.get("gender", String.class);
            Integer userAge = claims.get("age", Integer.class);
            
            // Watch 채팅은 익명이므로 nickname 없음
            String userNickname = null;
            
            if ("JOIN".equals(action)) {
                handleWatchChatRoomJoin(requestId, userId, userTeamId, userGender, userAge, userNickname, requestNode);
            } else {
                log.warn("Watch 채팅에서는 JOIN 액션만 지원됩니다: requestId={}, action={}", requestId, action);
                throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
            }
            
        } catch (ApiException e) {
            log.warn("Watch 채팅 요청 처리 실패 - ApiException: requestId={}, error={}", requestId, e.getMessage());
            sendErrorResponse(requestId, e.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error("Watch 채팅 요청 처리 실패 - Exception: requestId={}", requestId, e);
            sendErrorResponse(requestId, "서버 오류가 발생했습니다.");
        }
    }
    
    /**
     * Watch 채팅방 입장 요청 처리
     */
    private void handleWatchChatRoomJoin(String requestId, Long userId, Long userTeamId, 
                                       String userGender, Integer userAge, String userNickname, JsonNode requestNode) {
        try {
            // 기존 ChatAuthService 로직 사용
            ChatAuthRequest chatAuthRequest = createChatAuthRequestFromNode(requestNode);
            var response = chatAuthService.authorizeChatAccess(userId, userTeamId, userGender, userAge, userNickname, chatAuthRequest);
            
            log.info("Watch 채팅방 입장 처리 완료: requestId={}, success={}", requestId, response.status() == Status.SUCCESS);
            
        } catch (ApiException e) {
            log.warn("Watch 채팅방 입장 처리 실패: requestId={}, error={}", requestId, e.getMessage());
            sendErrorResponse(requestId, e.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error("Watch 채팅방 입장 처리 중 오류: requestId={}", requestId, e);
            sendErrorResponse(requestId, "서버 오류가 발생했습니다.");
        }
    }
    
    /**
     * 실패 응답 전송
     */
    private void sendErrorResponse(String requestId, String errorMessage) {
        try {
            Map<String, Object> authResult = new HashMap<>();
            authResult.put("success", false);
            authResult.put("requestId", requestId);
            authResult.put("timestamp", LocalDateTime.now().toString());
            authResult.put("errorMessage", errorMessage);
            
            chatAuthKafkaProducer.sendAuthResult(requestId, authResult);
            
        } catch (Exception e) {
            log.error("실패 응답 전송 실패: requestId={}", requestId, e);
        }
    }
    
    /**
     * JsonNode를 ChatAuthRequest로 변환
     */
    private ChatAuthRequest createChatAuthRequestFromNode(JsonNode node) {
        try {
            Map<String, Object> roomInfo = null;
            if (node.has("roomInfo") && !node.get("roomInfo").isNull()) {
                roomInfo = objectMapper.convertValue(node.get("roomInfo"), Map.class);
            }

            String action = node.get("action").asText();

            return ChatAuthRequest.builder()
                    .requestId(node.get("requestId").asText())
                    .chatType(node.has("chatType") ? node.get("chatType").asText() : "WATCH")
                    .action(action)
                    .roomId(getRoomIdByAction(node, action))  // matchId 대신 roomId 사용
                    .gameId(node.has("gameId") ? node.get("gameId").asLong() : null)
                    .teamId(roomInfo != null && roomInfo.containsKey("teamId") ? 
                        ((Number) roomInfo.get("teamId")).longValue() : null)
                    .roomInfo(roomInfo)
                    .build();

        } catch (Exception e) {
            log.error("ChatAuthRequest 변환 실패: {}", node.toString(), e);
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
    /**
     * 액션에 따라 적절한 roomId 반환
     */
    private String getRoomIdByAction(JsonNode node, String action) {
        if ("CREATE".equals(action)) {
            return String.valueOf(node.get("gameId").asLong());
        } else if ("JOIN".equals(action)) {
            if (node.has("roomId") && !node.get("roomId").isNull()) {
                return node.get("roomId").asText();
            } else {
                return String.valueOf(node.get("gameId").asLong());
            }
        }
        return String.valueOf(node.get("gameId").asLong());
    }
}
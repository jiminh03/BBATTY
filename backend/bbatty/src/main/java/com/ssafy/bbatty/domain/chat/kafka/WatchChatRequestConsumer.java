package com.ssafy.bbatty.domain.chat.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.chat.config.ChatKafkaProperties;
import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.service.ChatAuthService;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.security.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 관전 채팅 관련 요청을 처리하는 Kafka Consumer
 * AbstractChatRequestConsumer를 상속받아 공통 로직 재사용
 */
@Component
@Slf4j
public class WatchChatRequestConsumer extends AbstractChatRequestConsumer {
    
    private final ChatKafkaProperties kafkaProperties;

    public WatchChatRequestConsumer(ObjectMapper objectMapper,
                                   JwtProvider jwtProvider,
                                   ChatAuthService chatAuthService,
                                   ChatAuthKafkaProducer chatAuthKafkaProducer,
                                   ChatKafkaProperties kafkaProperties) {
        super(objectMapper, jwtProvider, chatAuthService, chatAuthKafkaProducer);
        this.kafkaProperties = kafkaProperties;
    }
    
    @KafkaListener(topics = "#{chatKafkaProperties.topics.watchChatRequest}", 
                   groupId = "#{chatKafkaProperties.groups.watchChatGroup}")
    public void handleWatchChatRequest(@Payload String message,
                                     @Header("Authorization") String authHeader) {
        super.handleChatRequest(message, authHeader, "WATCH");
    }

    @Override
    protected ChatAuthRequest createChatAuthRequest(JsonNode requestNode, String requestId, String chatType) {
        try {
            Map<String, Object> roomInfo = null;
            if (requestNode.has("roomInfo") && !requestNode.get("roomInfo").isNull()) {
                roomInfo = objectMapper.convertValue(requestNode.get("roomInfo"), Map.class);
            }

            String action = requestNode.get("action").asText();
            Long gameId = requestNode.has("gameId") ? requestNode.get("gameId").asLong() : null;
            Long teamId = roomInfo != null && roomInfo.containsKey("teamId") ?
                    ((Number) roomInfo.get("teamId")).longValue() : null;

            return ChatAuthRequest.builder()
                    .requestId(requestId)
                    .chatType(chatType)
                    .action(action)
                    .roomId(getRoomIdByAction(requestNode, action))
                    .gameId(gameId)
                    .teamId(teamId)
                    .roomInfo(roomInfo)
                    .build();

        } catch (Exception e) {
            log.error("WATCH 채팅 요청 변환 실패: requestId={}", requestId, e);
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
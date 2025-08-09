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
 * 매칭 채팅 관련 요청을 처리하는 Kafka Consumer
 * AbstractChatRequestConsumer를 상속받아 공통 로직 재사용
 */
@Component
@Slf4j
public class MatchChatRequestConsumer extends AbstractChatRequestConsumer {

    private final ChatKafkaProperties kafkaProperties;

    public MatchChatRequestConsumer(ObjectMapper objectMapper,
                                    JwtProvider jwtProvider,
                                    ChatAuthService chatAuthService,
                                    ChatAuthKafkaProducer chatAuthKafkaProducer,
                                    ChatKafkaProperties kafkaProperties) {
        super(objectMapper, jwtProvider, chatAuthService, chatAuthKafkaProducer);
        this.kafkaProperties = kafkaProperties;
    }

    @KafkaListener(topics = "#{chatKafkaProperties.topics.matchChatRequest}", 
                   groupId = "#{chatKafkaProperties.groups.matchChatGroup}")
    public void handleMatchChatRequest(@Payload String message,
                                       @Header("Authorization") String authHeader) {
        super.handleChatRequest(message, authHeader, "MATCH");
    }

    @Override
    protected ChatAuthRequest createChatAuthRequest(JsonNode requestNode, String requestId, String chatType) {
        try {
            Map<String, Object> roomInfo = null;
            if (requestNode.has("roomInfo") && !requestNode.get("roomInfo").isNull()) {
                roomInfo = objectMapper.convertValue(requestNode.get("roomInfo"), Map.class);
            }

            String action = requestNode.get("action").asText();
            Long gameId = requestNode.get("gameId").asLong();

            return ChatAuthRequest.builder()
                    .requestId(requestId)
                    .chatType(chatType)
                    .action(action)
                    .roomId(getRoomIdByAction(requestNode, action))
                    .gameId(gameId)
                    .roomInfo(roomInfo)
                    .build();

        } catch (Exception e) {
            log.error("MATCH 채팅 요청 변환 실패: requestId={}", requestId, e);
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 액션에 따라 적절한 roomId 반환
     */
    private String getRoomIdByAction(JsonNode node, String action) {
        if ("CREATE".equals(action)) {
            // CREATE: gameId를 roomId로 사용 (새 채팅방 생성 시 게임 기반)
            return String.valueOf(node.get("gameId").asLong());
        } else if ("JOIN".equals(action)) {
            // JOIN: 실제 채팅방 ID가 있으면 사용, 없으면 gameId 사용
            if (node.has("roomId") && !node.get("roomId").isNull()) {
                return node.get("roomId").asText();
            } else {
                return String.valueOf(node.get("gameId").asLong());
            }
        }
        return String.valueOf(node.get("gameId").asLong()); // 기본값
    }
}

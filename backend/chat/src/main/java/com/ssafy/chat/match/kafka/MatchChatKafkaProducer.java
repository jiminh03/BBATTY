package com.ssafy.chat.match.kafka;

import com.ssafy.chat.common.util.KSTTimeUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.match.dto.MatchChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 매칭 채팅 kafka producer
 * kafkatemplate을 사용해 kafka 토픽 별로 메세지 발송을 담당하는 컴포넌트
 * matchId별로 토픽을 나누어 채팅방 단위로 메시지를 발송함
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchChatKafkaProducer {

    // kafka 메시지 전송을 위한 템플릿
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC_PREFIX = "match-chat-";
    /**
     * 채팅 메세지 발송
     */
    public void sendChatMessage(String matchId, MatchChatMessage message){
        try {
            String topicName = TOPIC_PREFIX + matchId;
            String messageJson = objectMapper.writeValueAsString(message);
            log.debug("kafka 채팅 메세지 발송 - topic: {}, userID: {}", topicName, message.getUserId());
            kafkaTemplate.send(topicName, matchId, messageJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("kafka 메세지 발송 실패 - topic: {}, matchID: {}", topicName, matchId, ex);
                        } else {
                            log.debug("kafka 메세지 발송 성공 - topic: {}, offset: {}", topicName, result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("매칭 채팅 메세지 직렬화 실패 - matchID: {}", matchId, e);
        }
    }
    /**
     * 사용자 입/퇴장 이벤트 발송
     */
    public void sendEvent(String matchId, Map<String, Object> event){
        try {
            String topicName = TOPIC_PREFIX + matchId;
            String eventJson = objectMapper.writeValueAsString(event);
            log.debug("kafka 이벤트 발송 - topic: {}, type: {}", topicName, event.get("type"));
            kafkaTemplate.send(topicName, matchId, eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("kafka 이벤트 발송 실패 - topic: {}, matchId: {}", topicName, matchId, ex);
                        } else {
                            log.debug("kafka 이벤트 발송 성공 - topic: {}, offset: {}", topicName, result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e){
            log.error("매칭 채팅 이벤트 직렬화 실패 - matchId: {}", matchId, e);
        }
    }
    /**
     * 사용자 입장 이벤트 발송
     */
    public void sendUserJoinEvent(String matchId, Long userId, String userName){
        Map<String, Object> joinEvent = Map.of(
                "messageType", "USER_JOIN",
                "roomId", matchId,
                "timestamp", KSTTimeUtil.nowAsString(),
                "userId", userId,
                "userName", userName,
                "content", userName + "님이 입장하셨습니다."
        );
        sendEvent(matchId, joinEvent);
    }

    /**
     * 사용자 퇴장 이벤트 발송
     */
    public void sendUserLeaveEvent(String matchId, Long userId, String userName){
        Map<String, Object> leaveEvent = Map.of(
                "messageType", "USER_LEAVE",
                "roomId", matchId,
                "timestamp", KSTTimeUtil.nowAsString(),
                "userId", userId,
                "userName", userName,
                "content", userName + "님이 퇴장하셨습니다."
        );
        sendEvent(matchId, leaveEvent);
    }


}

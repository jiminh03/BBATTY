package com.ssafy.chat.watch.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.util.KSTTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 관전 채팅 알림 Kafka Producer
 * 채팅방 트래픽 급증 이벤트를 bbatty 서버로 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WatchChatNotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC_NAME = "team-fire-alert";

    /**
     * 팀 채팅방 "불이 났어요" 이벤트 발송
     */
    public void sendTeamFireAlert(Long teamId) {
        try {
            Map<String, Object> fireEvent = Map.of(
                    "alertType", "TRAFFIC_SPIKE",
                    "teamId", teamId,
                    "timestamp", KSTTimeUtil.nowAsTimestamp()
            );
            
            String eventJson = objectMapper.writeValueAsString(fireEvent);
            
            kafkaTemplate.send(TOPIC_NAME, teamId.toString(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("팀 불이 났어요 이벤트 발송 실패 - teamId: {}", teamId, ex);
                        } else {
                            log.info("팀 불이 났어요 이벤트 발송 성공 - teamId: {}", teamId);
                        }
                    });
            
        } catch (Exception e) {
            log.error("팀 불이 났어요 이벤트 직렬화 실패 - teamId: {}", teamId, e);
        }
    }
}
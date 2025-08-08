package com.ssafy.schedule.domain.chat.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.schedule.domain.chat.dto.ChatRoomCreateEventDto;
import com.ssafy.schedule.domain.chat.dto.GameListEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 채팅 관련 이벤트를 Kafka로 전송하는 Producer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventKafkaProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CHAT_ROOM_CREATE_TOPIC = "chat-room-create-events";
    private static final String GAME_LIST_UPDATE_TOPIC = "game-list-update-events";
    
    /**
     * 채팅방 생성 이벤트를 Kafka로 전송
     */
    public void sendChatRoomCreateEvent(ChatRoomCreateEventDto eventDto) {
        try {
            String messageJson = objectMapper.writeValueAsString(eventDto);
            String key = String.valueOf(eventDto.getGameId());
            
            log.debug("채팅방 생성 이벤트 Kafka 전송: gameId={}, homeTeam={} vs awayTeam={}", 
                eventDto.getGameId(), eventDto.getHomeTeamName(), eventDto.getAwayTeamName());
            
            kafkaTemplate.send(CHAT_ROOM_CREATE_TOPIC, key, messageJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("채팅방 생성 이벤트 Kafka 전송 실패: gameId={}, error={}", 
                                    eventDto.getGameId(), ex.getMessage(), ex);
                        } else {
                            log.info("✅ 채팅방 생성 이벤트 Kafka 전송 성공: gameId={}, offset={}", 
                                    eventDto.getGameId(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("채팅방 생성 이벤트 직렬화 실패: gameId={}", eventDto.getGameId(), e);
        }
    }
    
    /**
     * 게임 리스트 업데이트 이벤트를 Kafka로 전송
     */
    public void sendGameListUpdateEvent(GameListEventDto eventDto) {
        try {
            String messageJson = objectMapper.writeValueAsString(eventDto);
            String key = "game-list-update";
            
            log.debug("게임 리스트 업데이트 이벤트 Kafka 전송: totalCount={}", eventDto.getTotalCount());
            
            kafkaTemplate.send(GAME_LIST_UPDATE_TOPIC, key, messageJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("게임 리스트 업데이트 이벤트 Kafka 전송 실패: totalCount={}, error={}", 
                                    eventDto.getTotalCount(), ex.getMessage(), ex);
                        } else {
                            log.info("✅ 게임 리스트 업데이트 이벤트 Kafka 전송 성공: totalCount={}, offset={}", 
                                    eventDto.getTotalCount(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("게임 리스트 업데이트 이벤트 직렬화 실패: totalCount={}", eventDto.getTotalCount(), e);
        }
    }
}
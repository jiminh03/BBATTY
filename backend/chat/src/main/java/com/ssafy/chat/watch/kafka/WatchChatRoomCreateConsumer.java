package com.ssafy.chat.watch.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.watch.dto.ChatRoomCreateEventDto;
import com.ssafy.chat.watch.service.WatchChatRoomService;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 채팅방 생성 이벤트를 수신하여 관전 채팅방을 자동 생성하는 Consumer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WatchChatRoomCreateConsumer {

    private final WatchChatRoomService watchChatRoomService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "chat-room-create-events",
            groupId = "watch-chat-room-consumer-group"
    )
    public void handleChatRoomCreateEvent(
            @Payload String messageJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset) {

        try {
            log.info("🏟️ 채팅방 생성 이벤트 수신 - topic: {}, offset: {}", topic, offset);
            log.debug("🔍 수신된 원본 JSON: {}", messageJson);

            // JSON 파싱
            ChatRoomCreateEventDto eventDto = objectMapper.readValue(messageJson, ChatRoomCreateEventDto.class);

            log.info("🏟️ 이벤트 파싱 완료 - gameId: {}, 경기: {} vs {}",
                    eventDto.getGameId(), eventDto.getHomeTeamName(), eventDto.getAwayTeamName());

            // 관전 채팅방 자동 생성
            createWatchChatRooms(eventDto);

            log.info("✅ 관전 채팅방 생성 처리 완료 - gameId: {}", eventDto.getGameId());

        } catch (JsonProcessingException e) {
            log.error("🚨 이벤트 JSON 파싱 실패 - message: {}", messageJson, e);
            throw new ApiException(ErrorCode.JSON_DESERIALIZATION_FAILED);
        } catch (ApiException e) {
            log.error("🚨 관전 채팅방 생성 실패 - ErrorCode: {}", e.getErrorCode(), e);
            throw e;
        } catch (Exception e) {
            log.error("🚨 예상치 못한 오류 발생 - message: {}", messageJson, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }

    /**
     * 경기별 관전 채팅방들 생성 (팀별만)
     */
    private void createWatchChatRooms(ChatRoomCreateEventDto eventDto) {
        try {
            // 1. 홈팀 관전 채팅방 생성
            String homeWatchRoomId = createHomeTeamWatchRoom(eventDto);

            // 2. 원정팀 관전 채팅방 생성
            String awayWatchRoomId = createAwayTeamWatchRoom(eventDto);

            log.info("🏟️ 팀별 관전 채팅방 생성 완료 - gameId: {}, rooms: [home: {}, away: {}]",
                    eventDto.getGameId(), homeWatchRoomId, awayWatchRoomId);

        } catch (Exception e) {
            log.error("관전 채팅방 생성 중 오류 - gameId: {}", eventDto.getGameId(), e);
            throw new ApiException(ErrorCode.WATCH_CHAT_ROOM_CREATE_FAILED);
        }
    }

    /**
     * 홈팀 관전 채팅방 생성
     */
    private String createHomeTeamWatchRoom(ChatRoomCreateEventDto eventDto) {
        try {
            String roomName = String.format("%s 응원방", eventDto.getHomeTeamName());

            String roomId = watchChatRoomService.createWatchChatRoom(
                    eventDto.getGameId(),
                    roomName,
                    "TEAM",  // 팀별 관전
                    eventDto.getHomeTeamId(),
                    eventDto
            );

            log.debug("홈팀 관전 채팅방 생성 완료 - roomId: {}", roomId);
            return roomId;

        } catch (Exception e) {
            log.error("홈팀 관전 채팅방 생성 실패 - gameId: {}, homeTeamId: {}",
                    eventDto.getGameId(), eventDto.getHomeTeamId(), e);
            throw e;
        }
    }

    /**
     * 원정팀 관전 채팅방 생성
     */
    private String createAwayTeamWatchRoom(ChatRoomCreateEventDto eventDto) {
        try {
            String roomName = String.format("%s 응원방", eventDto.getAwayTeamName());

            String roomId = watchChatRoomService.createWatchChatRoom(
                    eventDto.getGameId(),
                    roomName,
                    "TEAM",  // 팀별 관전
                    eventDto.getAwayTeamId(),
                    eventDto
            );

            log.debug("원정팀 관전 채팅방 생성 완료 - roomId: {}", roomId);
            return roomId;

        } catch (Exception e) {
            log.error("원정팀 관전 채팅방 생성 실패 - gameId: {}, awayTeamId: {}",
                    eventDto.getGameId(), eventDto.getAwayTeamId(), e);
            throw e;
        }
    }
}

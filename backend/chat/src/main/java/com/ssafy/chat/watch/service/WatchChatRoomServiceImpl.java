package com.ssafy.chat.watch.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.watch.dto.ChatRoomCreateEventDto;
import com.ssafy.chat.watch.dto.WatchChatRoom;
import com.ssafy.chat.watch.service.WatchChatRoomService;
import com.ssafy.chat.global.constants.ChatRedisKey;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchChatRoomServiceImpl implements WatchChatRoomService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String createWatchChatRoom(Long gameId, String roomName, String chatType,
                                      Long teamId, ChatRoomCreateEventDto eventDto) {
        try {
            // 중복 생성 방지
            String roomId = generateWatchRoomId(gameId, chatType, teamId);
            if (isWatchRoomExists(roomId)) {
                log.warn("이미 존재하는 관전 채팅방 - roomId: {}", roomId);
                return roomId;
            }

            // 관전 채팅방 데이터 구성
            WatchChatRoom watchRoom = WatchChatRoom.builder()
                    .roomId(roomId)
                    .gameId(gameId)
                    .roomName(roomName)
                    .chatType(chatType)
                    .teamId(teamId)
                    .gameDate(eventDto.getDateTime().format(DATE_FORMATTER))
                    .stadium(eventDto.getStadium())
                    .homeTeamId(eventDto.getHomeTeamId())
                    .homeTeamName(eventDto.getHomeTeamName())
                    .awayTeamId(eventDto.getAwayTeamId())
                    .awayTeamName(eventDto.getAwayTeamName())
                    .currentParticipants(0)
                    .maxParticipants(1000)  // 관전방은 대용량
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now().toString())
                    .build();

            // Redis에 저장
            String roomKey = ChatRedisKey.getWatchRoomInfoKey(roomId);
            String roomJson = objectMapper.writeValueAsString(watchRoom);

            // TTL 계산 (경기 날짜 + 1일)
            Duration ttl = calculateTTLFromGameDate(eventDto.getDateTime().format(DATE_FORMATTER));
            redisTemplate.opsForValue().set(roomKey, roomJson, ttl);

            log.info("관전 채팅방 생성 완료 - roomId: {}, roomName: {}", roomId, roomName);

            return roomId;

        } catch (Exception e) {
            log.error("관전 채팅방 생성 실패 - gameId: {}, roomName: {}", gameId, roomName, e);
            throw new ApiException(ErrorCode.WATCH_CHAT_ROOM_CREATE_FAILED);
        }
    }

    /**
     * 관전 채팅방 ID 생성
     */
    private String generateWatchRoomId(Long gameId, String chatType, Long teamId) {
        return String.format("watch_%d_team_%d", gameId, teamId);
    }

    /**
     * 관전 채팅방 존재 여부 확인
     */
    private boolean isWatchRoomExists(String roomId) {
        String roomKey = ChatRedisKey.getWatchRoomInfoKey(roomId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(roomKey));
    }

    /**
     * 게임 날짜를 기준으로 TTL 계산
     */
    private Duration calculateTTLFromGameDate(String gameDateStr) {
        try {
            LocalDate gameDate = LocalDate.parse(gameDateStr, DATE_FORMATTER);
            LocalDateTime endOfGameDay = gameDate.plusDays(1).atTime(LocalTime.MIDNIGHT);

            Duration ttl = Duration.between(LocalDateTime.now(), endOfGameDay);

            // TTL이 음수이거나 너무 짧으면 기본 24시간 설정
            if (ttl.isNegative() || ttl.toHours() < 1) {
                ttl = Duration.ofHours(24);
            }

            return ttl;
        } catch (Exception e) {
            log.warn("게임 날짜 파싱 실패 - gameDateStr: {}", gameDateStr, e);
            return Duration.ofHours(24); // 기본값
        }
    }
}

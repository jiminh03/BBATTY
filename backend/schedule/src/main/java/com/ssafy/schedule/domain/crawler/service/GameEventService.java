package com.ssafy.schedule.domain.crawler.service;

import com.ssafy.schedule.domain.chat.dto.ChatRoomCreateEventDto;
import com.ssafy.schedule.domain.chat.kafka.ChatEventKafkaProducer;
import com.ssafy.schedule.global.entity.Game;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 경기 관련 이벤트 처리 서비스
 * - 경기 시작 3시간 전 알림 이벤트
 * - 채팅 서버로 이벤트 전송 (추후 구현)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameEventService {
    
    private final ChatEventKafkaProducer chatEventKafkaProducer;

    /**
     * 경기 시작 3시간 전 이벤트 처리
     * @param game 이벤트 대상 경기
     */
    public void handleGameStartingSoonEvent(Game game) {
        log.info("🔔 경기 시작 3시간 전 이벤트 발생!");
        log.info("경기 정보: {} vs {} at {}", 
                game.getAwayTeam().getName(), 
                game.getHomeTeam().getName(), 
                game.getDateTime());

        try {
            // 채팅 서버 이벤트 호출 시뮬레이션
            sendEventToChatServer(game);
            
        } catch (Exception e) {
            log.error("채팅 서버 이벤트 전송 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅 서버로 경기 이벤트 전송
     * Kafka를 통해 채팅방 생성 이벤트 전송
     * 
     * @param game 이벤트 대상 경기
     */
    private void sendEventToChatServer(Game game) {
        log.info("📡 채팅 서버로 채팅방 생성 이벤트 전송");
        log.info("이벤트 데이터: gameId={}, homeTeam={}, awayTeam={}, dateTime={}", 
                game.getId(),
                game.getHomeTeam().getName(),
                game.getAwayTeam().getName(),
                game.getDateTime());
        
        // DTO 생성 및 Kafka 전송
        ChatRoomCreateEventDto eventDto = ChatRoomCreateEventDto.from(game);
        chatEventKafkaProducer.sendChatRoomCreateEvent(eventDto);
    }
}
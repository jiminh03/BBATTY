package com.ssafy.schedule.service;

import com.ssafy.schedule.entity.Game;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 경기 관련 이벤트 처리 서비스
 * - 경기 시작 2시간 전 알림 이벤트
 * - 채팅 서버로 이벤트 전송 (추후 구현)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameEventService {

    /**
     * 경기 시작 2시간 전 이벤트 처리
     * - 현재는 로그만 출력하고, 추후 채팅 서버 호출 로직 추가 예정
     * 
     * @param game 이벤트 대상 경기
     */
    public void handleGameStartingSoonEvent(Game game) {
        log.info("🔔 경기 시작 2시간 전 이벤트 발생!");
        log.info("경기 정보: {} vs {} at {}", 
                game.getAwayTeam().getName(), 
                game.getHomeTeam().getName(), 
                game.getDateTime());
        
        // TODO: 추후 채팅 서버로 이벤트 전송 로직 구현
        // - REST API 호출
        // - Message Queue 발행
        // - WebSocket 알림 등
        
        try {
            // 채팅 서버 이벤트 호출 시뮬레이션
            sendEventToChatServer(game);
            
        } catch (Exception e) {
            log.error("채팅 서버 이벤트 전송 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅 서버로 경기 이벤트 전송
     * - 현재는 시뮬레이션만 구현
     * - 실제 채팅 서버 API가 정해지면 구현 예정
     * 
     * @param game 이벤트 대상 경기
     */
    private void sendEventToChatServer(Game game) {
        // TODO: 실제 채팅 서버 API 호출 구현
        log.info("📡 채팅 서버로 이벤트 전송 시뮬레이션");
        log.info("이벤트 데이터: gameId={}, homeTeam={}, awayTeam={}, dateTime={}", 
                game.getId(),
                game.getHomeTeam().getName(),
                game.getAwayTeam().getName(),
                game.getDateTime());
        
        // 실제 구현 예시:
        // RestTemplate restTemplate = new RestTemplate();
        // ChatEventRequest request = new ChatEventRequest(game);
        // restTemplate.postForObject("http://chat-server/api/events/game-starting", request, Void.class);
        
        // 또는 Message Queue 사용:
        // rabbitTemplate.convertAndSend("game.events", "game.starting", gameEventMessage);
    }
}
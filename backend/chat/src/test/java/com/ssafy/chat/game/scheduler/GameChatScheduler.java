package com.ssafy.bbatty.domain.chat.game.scheduler;

import com.ssafy.bbatty.domain.chat.game.service.GameChatRoomService;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 경기 채팅방 자동 활성화/비활성화 스케줄러
 * - 경기 시작 2시간 전부터 자정까지 활성화
 * - 매일 자정 크롤링 데이터 갱신 후 채팅방 상태 업데이트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameChatScheduler {

    private final GameChatRoomService gameChatRoomService;
    private final GameService gameService;

    /**
     * 경기 채팅방 상태 관리 (1분마다 실행)
     * - 경기 시작 2시간 전: 활성화
     * - 자정 12시: 비활성화
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void manageGameChatRooms() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Game> todayGames = gameService.getTodayGames();
            
            log.debug("경기 채팅방 상태 관리 시작 - 현재시간: {}, 오늘 경기 수: {}", now, todayGames.size());
            
            for (Game game : todayGames) {
                manageGameChatRoom(game, now);
            }
            
        } catch (Exception e) {
            log.error("경기 채팅방 상태 관리 중 오류 발생", e);
        }
    }

    /**
     * 개별 경기 채팅방 상태 관리
     */
    private void manageGameChatRoom(Game game, LocalDateTime now) {
        try {
            LocalDateTime gameStartTime = game.getGameDateTime();
            LocalDateTime activateTime = gameStartTime.minusHours(2); // 2시간 전
            LocalDateTime deactivateTime = LocalDateTime.of(gameStartTime.toLocalDate().plusDays(1), LocalTime.MIDNIGHT);
            
            boolean shouldBeActive = now.isAfter(activateTime) && now.isBefore(deactivateTime);
            boolean currentlyActive = gameChatRoomService.isChatRoomActiveByGameId(game.getId());
            
            if (shouldBeActive && !currentlyActive) {
                // 활성화 필요
                activateGameChatRoom(game);
            } else if (!shouldBeActive && currentlyActive) {
                // 비활성화 필요
                deactivateGameChatRoom(game);
            }
            
        } catch (Exception e) {
            log.error("개별 경기 채팅방 상태 관리 실패 - gameId: {}", game.getId(), e);
        }
    }

    /**
     * 경기 채팅방 활성화
     */
    private void activateGameChatRoom(Game game) {
        try {
            // 채팅방 생성 (존재하지 않는 경우)
            gameChatRoomService.createTeamChatRooms(game.getId());
            
            // 활성화
            gameChatRoomService.activateGameChatRooms(game.getId());
            
            // 더블헤더 처리
            if (gameService.isDoubleHeader(game)) {
                Game secondGame = gameService.getSecondGameOfDoubleHeader(game);
                if (secondGame != null) {
                    gameChatRoomService.mergeDoubleHeaderChatRooms(game.getId(), secondGame.getId());
                    log.info("더블헤더 채팅방 통합 완료 - game1: {}, game2: {}", game.getId(), secondGame.getId());
                }
            }
            
            log.info("경기 채팅방 활성화 완료 - gameId: {}, 경기: {} vs {}", 
                    game.getId(), game.getHomeTeam().getName(), game.getAwayTeam().getName());
                    
        } catch (Exception e) {
            log.error("경기 채팅방 활성화 실패 - gameId: {}", game.getId(), e);
        }
    }

    /**
     * 경기 채팅방 비활성화
     */
    private void deactivateGameChatRoom(Game game) {
        try {
            gameChatRoomService.deactivateGameChatRooms(game.getId());
            
            log.info("경기 채팅방 비활성화 완료 - gameId: {}, 경기: {} vs {}", 
                    game.getId(), game.getHomeTeam().getName(), game.getAwayTeam().getName());
                    
        } catch (Exception e) {
            log.error("경기 채팅방 비활성화 실패 - gameId: {}", game.getId(), e);
        }
    }

    /**
     * 매일 자정 12시 - 경기 정보 갱신 후 채팅방 초기화
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    public void dailyGameChatRoomReset() {
        try {
            log.info("일일 경기 채팅방 초기화 시작");
            
            // 모든 활성 채팅방 비활성화
            gameChatRoomService.deactivateAllGameChatRooms();
            
            // 어제 경기 채팅방 정리
            gameChatRoomService.cleanupExpiredGameChatRooms();
            
            // 직관 인증 정보 초기화 (자정에 리셋)
            gameChatRoomService.resetAttendanceAuthentication();
            
            log.info("일일 경기 채팅방 초기화 완료");
            
        } catch (Exception e) {
            log.error("일일 경기 채팅방 초기화 실패", e);
        }
    }

    /**
     * 15분마다 채팅방 건강성 체크
     */
    @Scheduled(fixedRate = 900000) // 15분마다
    public void healthCheckGameChatRooms() {
        try {
            log.debug("경기 채팅방 건강성 체크 시작");
            
            // Redis 연결 상태 확인
            gameChatRoomService.healthCheckRedisConnection();
            
            // 좀비 채팅방 정리 (활성화되어 있지만 사용자가 없는 방)
            gameChatRoomService.cleanupZombieChatRooms();
            
            log.debug("경기 채팅방 건강성 체크 완료");
            
        } catch (Exception e) {
            log.warn("경기 채팅방 건강성 체크 중 오류", e);
        }
    }
}
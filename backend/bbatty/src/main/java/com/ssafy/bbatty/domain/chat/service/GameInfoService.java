package com.ssafy.bbatty.domain.chat.service;

import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 게임 정보 조회 전담 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GameInfoService {
    
    private final GameRepository gameRepository;
    
    /**
     * 게임 정보 생성 (매칭 채팅용)
     */
    public Map<String, Object> createGameInfo(Long gameId) {
        try {
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
            
            return Map.of(
                    "gameId", game.getId(),
                    "gameDate", game.getDateTime().toString(), // LocalDate -> "yyyy-MM-dd"
                    "homeTeamId", game.getHomeTeamId(),
                    "awayTeamId", game.getAwayTeamId(),
                    "homeTeamName", game.getHomeTeam() != null ? game.getHomeTeam().getName() : "홈팀",
                    "awayTeamName", game.getAwayTeam() != null ? game.getAwayTeam().getName() : "원정팀",
                    "status", game.getStatus().name(),
                    "stadium", game.getStadium() != null ? game.getStadium() : "경기장"
            );
        } catch (Exception e) {
            log.error("게임 정보 생성 실패 - gameId: {}", gameId, e);
            throw new ApiException(ErrorCode.GAME_NOT_FOUND);
        }
    }
    
    /**
     * 게임 존재 여부 확인
     */
    public Game findGameById(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));
    }
}
package com.ssafy.chat.match.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.dto.GameInfo;
import com.ssafy.chat.match.dto.GameInfoList;
import com.ssafy.chat.match.dto.GameListResponse;
import com.ssafy.chat.match.dto.GameInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameInfoServiceImpl implements GameInfoService {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final MatchChatService matchChatService;
    
    private static final String GAME_KEY_PREFIX = "games:";
    private static final Duration TTL_DURATION = Duration.ofDays(14); // 2주 자동 삭제
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void processGameInfoMessage(String message) {
        try {
            GameInfoList gameInfoList = objectMapper.readValue(message, GameInfoList.class);
            saveGameInfoList(gameInfoList.getGames());
            log.info("Successfully processed {} game infos", gameInfoList.getGames().size());
        } catch (Exception e) {
            log.error("Failed to process game info message: {}", message, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }

    @Override
    public void saveGameInfoList(List<GameInfo> gameInfos) {
        for (GameInfo gameInfo : gameInfos) {
            saveGameInfo(gameInfo);
        }
    }

    private void saveGameInfo(GameInfo gameInfo) {
        try {
            String date = gameInfo.getDateTime().format(DATE_FORMATTER);
            String key = GAME_KEY_PREFIX + date;
            
            List<GameInfo> existingGames = getGameInfosByDate(date);
            
            boolean exists = existingGames.stream()
                    .anyMatch(game -> game.getGameId().equals(gameInfo.getGameId()));
            
            if (!exists) {
                existingGames.add(gameInfo);
                String jsonValue = objectMapper.writeValueAsString(existingGames);
                redisUtil.setValue(key, jsonValue, TTL_DURATION);
                log.info("Game info saved for date: {}, gameId: {}", date, gameInfo.getGameId());
            }
        } catch (Exception e) {
            log.error("Failed to save game info: {}", gameInfo.getGameId(), e);
        }
    }

    @Override
    public List<GameInfo> getGameInfosByDate(String date) {
        try {
            String key = GAME_KEY_PREFIX + date;
            String jsonValue = redisUtil.getValue(key, String.class);
            
            if (jsonValue == null) {
                return new ArrayList<>();
            }
            
            return objectMapper.readValue(jsonValue, new TypeReference<List<GameInfo>>() {});
        } catch (Exception e) {
            log.error("Failed to get game info for date: {}", date, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public GameListResponse getGamesListByDate(String date) {
        List<GameInfo> games = getGameInfosByDate(date);
        
        List<GameInfoResponse> gameResponses = games.stream()
                .map(game -> {
                    int activeUserCount = matchChatService.getActiveSessionCount(game.getGameId().toString());
                    return GameInfoResponse.from(game, activeUserCount);
                })
                .collect(Collectors.toList());
        
        return new GameListResponse(date, gameResponses);
    }
    
    @Override
    public List<GameListResponse> getGamesListByDateRange(String startDate, String endDate) {
        List<GameListResponse> result = new ArrayList<>();
        
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        
        LocalDate current = start;
        while (!current.isAfter(end)) {
            String dateStr = current.format(DATE_FORMATTER);
            GameListResponse gameList = getGamesListByDate(dateStr);
            
            if (!gameList.getGames().isEmpty()) {
                result.add(gameList);
            }
            
            current = current.plusDays(1);
        }
        
        return result;
    }
}
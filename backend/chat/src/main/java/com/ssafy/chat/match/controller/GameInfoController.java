package com.ssafy.chat.match.controller;

import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.response.ApiResponse;
import com.ssafy.chat.match.dto.GameListResponse;
import com.ssafy.chat.match.service.GameInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/match/games")
@RequiredArgsConstructor
@Slf4j
public class GameInfoController {
    
    private final GameInfoService gameInfoService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping("/{date}")
    public ApiResponse<GameListResponse> getGamesByDate(@PathVariable String date) {
        try {
            LocalDate.parse(date, DATE_FORMATTER);
            GameListResponse gameList = gameInfoService.getGamesListByDate(date);
            return ApiResponse.success(gameList);
        } catch (Exception e) {
            log.error("Failed to get games for date: {}", date, e);
            return ApiResponse.fail(ErrorCode.SERVER_ERROR, null);
        }
    }

    @GetMapping
    public ApiResponse<List<GameListResponse>> getGamesInRange(
            @RequestParam(defaultValue = "0") int days) {
        try {
            LocalDate today = LocalDate.now();
            String startDate = today.format(DATE_FORMATTER);
            String endDate = today.plusDays(days == 0 ? 13 : days - 1).format(DATE_FORMATTER);
            
            List<GameListResponse> gameList = gameInfoService.getGamesListByDateRange(startDate, endDate);
            return ApiResponse.success(gameList);
        } catch (Exception e) {
            log.error("Failed to get games in range", e);
            return ApiResponse.fail(ErrorCode.SERVER_ERROR, null);
        }
    }
    
    @GetMapping("/range")
    public ApiResponse<List<GameListResponse>> getGamesByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate.parse(endDate, DATE_FORMATTER);
            
            List<GameListResponse> gameList = gameInfoService.getGamesListByDateRange(startDate, endDate);
            return ApiResponse.success(gameList);
        } catch (Exception e) {
            log.error("Failed to get games for date range: {} to {}", startDate, endDate, e);
            return ApiResponse.fail(ErrorCode.SERVER_ERROR, null);
        }
    }
}
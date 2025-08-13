package com.ssafy.bbatty.domain.game.controller;

import com.ssafy.bbatty.domain.game.dto.response.GameScheduleResponse;
import com.ssafy.bbatty.domain.game.service.GameService;
import com.ssafy.bbatty.global.constants.SuccessCode;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.SecurityUtils;
import com.ssafy.bbatty.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 경기 정보 컨트롤러
 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;

    /**
     * 특정 날짜 기준 3주간 경기 일정 조회
     */
    @GetMapping("/three-weeks")
    public ResponseEntity<ApiResponse<List<GameScheduleResponse>>> getThreeWeekSchedule() {

        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        LocalDate date = LocalDate.now(seoulZone).plusDays(1);;

        log.info("3주간 경기 일정 조회 요청: {}", date);
        
        List<GameScheduleResponse> schedule = gameService.getThreeWeekSchedule(date);
        
        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, schedule));
    }

    /**
     * 사용자 팀의 오늘 경기 정보 조회
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<GameScheduleResponse>> getUserTeamTodayGame(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        GameScheduleResponse game = gameService.getUserTeamTodayGame(userPrincipal.getTeamId());
        
        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, game));
    }


    /**
     * gameId로 경기 정보 조회
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<ApiResponse<GameScheduleResponse>> getGameByGameId(@PathVariable Long gameId) {
        
        log.info("게임 ID {} 경기 조회 요청", gameId);
        
        GameScheduleResponse game = gameService.getGameByGameId(gameId);

        return ResponseEntity.status(SuccessCode.SUCCESS_DEFAULT.getStatus()).body(
                ApiResponse.success(SuccessCode.SUCCESS_DEFAULT, game));
    }

}
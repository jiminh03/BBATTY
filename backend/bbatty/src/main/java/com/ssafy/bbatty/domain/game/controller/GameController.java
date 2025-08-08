package com.ssafy.bbatty.domain.game.controller;

import com.ssafy.bbatty.domain.game.dto.response.GameScheduleResponse;
import com.ssafy.bbatty.domain.game.service.GameService;
import com.ssafy.bbatty.global.constants.SuccessCode;
import com.ssafy.bbatty.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
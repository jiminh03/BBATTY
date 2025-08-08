package com.ssafy.bbatty.domain.game.service;

import com.ssafy.bbatty.domain.game.dto.response.GameScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface GameService {
    
    /**
     * 특정 날짜 기준으로 3주간의 경기 일정 조회
     */
    List<GameScheduleResponse> getThreeWeekSchedule(LocalDate baseDate);
}

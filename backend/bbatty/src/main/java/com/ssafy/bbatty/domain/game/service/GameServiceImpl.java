package com.ssafy.bbatty.domain.game.service;

import com.ssafy.bbatty.domain.game.dto.response.GameScheduleResponse;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GameServiceImpl implements GameService {

    private final GameRepository gameRepository;

    @Override
    public List<GameScheduleResponse> getThreeWeekSchedule(LocalDate baseDate) {
        LocalDateTime startDateTime = baseDate.atStartOfDay();
        LocalDateTime endDateTime = baseDate.plusWeeks(3).atTime(LocalTime.MAX);
        
        log.info("3주간 경기 일정 조회: {} ~ {}", startDateTime, endDateTime);
        
        List<Game> games = gameRepository.findByDateTimeBetween(startDateTime, endDateTime);
        
        return games.stream()
                .map(GameScheduleResponse::from)
                .toList();
    }
}

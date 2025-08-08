package com.ssafy.bbatty.domain.game.dto.response;

import com.ssafy.bbatty.domain.game.entity.Game;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 경기 일정 응답 DTO
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameScheduleResponse {
    
    private Long gameId;
    private String awayTeamName;
    private String homeTeamName;
    private LocalDateTime dateTime;
    private String stadium;
    
    @Builder
    public GameScheduleResponse(Long gameId, String awayTeamName, String homeTeamName,
                                LocalDateTime dateTime, String stadium) {
        this.gameId = gameId;
        this.awayTeamName = awayTeamName;
        this.homeTeamName = homeTeamName;
        this.dateTime = dateTime;
        this.stadium = stadium;
    }
    
    public static GameScheduleResponse from(Game game) {
        return GameScheduleResponse.builder()
                .gameId(game.getId())
                .awayTeamName(game.getAwayTeam().getName())
                .homeTeamName(game.getHomeTeam().getName())
                .dateTime(game.getDateTime())
                .stadium(game.getStadium())
                .build();
    }
}
package com.ssafy.chat.match.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameInfoResponse {
    private Long gameId;
    private Long awayTeamId;
    private Long homeTeamId;
    private String awayTeamName;
    private String homeTeamName;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateTime;
    
    private String stadium;
    private int activeUserCount;
    
    public static GameInfoResponse from(GameInfo gameInfo, int activeUserCount) {
        return new GameInfoResponse(
            gameInfo.getGameId(),
            gameInfo.getAwayTeamId(),
            gameInfo.getHomeTeamId(),
            gameInfo.getAwayTeamName(),
            gameInfo.getHomeTeamName(),
            gameInfo.getStatus(),
            gameInfo.getDateTime(),
            gameInfo.getStadium(),
            activeUserCount
        );
    }
}
package com.ssafy.schedule.domain.chat.dto;

import com.ssafy.schedule.global.entity.Game;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateEventDto {
    
    private Long gameId;
    private Long awayTeamId;
    private Long homeTeamId;
    private String awayTeamName;
    private String homeTeamName;
    private LocalDateTime dateTime;
    private String stadium;
    
    /**
     * Game 엔티티로부터 ChatRoomCreateEventDto 생성
     */
    public static ChatRoomCreateEventDto from(Game game) {
        return new ChatRoomCreateEventDto(
                game.getId(),
                game.getAwayTeam().getId(),
                game.getHomeTeam().getId(),
                game.getAwayTeam().getName(),
                game.getHomeTeam().getName(),
                game.getDateTime(),
                game.getStadium()
        );
    }
}
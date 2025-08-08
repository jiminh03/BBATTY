package com.ssafy.schedule.domain.chat.dto;

import com.ssafy.schedule.global.entity.Game;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameListEventDto {
    
    private String eventType; // "GAME_LIST_UPDATE"
    private List<ChatRoomCreateEventDto> games;
    private int totalCount;
    
    /**
     * Game 엔티티 리스트로부터 GameListEventDto 생성
     */
    public static GameListEventDto from(List<Game> gameList) {
        List<ChatRoomCreateEventDto> gameDtos = gameList.stream()
                .map(ChatRoomCreateEventDto::from)
                .collect(Collectors.toList());
        
        return new GameListEventDto(
                "GAME_LIST_UPDATE",
                gameDtos,
                gameDtos.size()
        );
    }
}
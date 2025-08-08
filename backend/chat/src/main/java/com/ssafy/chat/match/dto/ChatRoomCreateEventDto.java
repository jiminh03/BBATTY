package com.ssafy.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateEventDto {
    private Long gameId;
    private String gameDate;        // "yyyy-MM-dd" 형식
    private String stadium;
    private Long homeTeamId;
    private String homeTeamName;
    private Long awayTeamId;
    private String awayTeamName;
    private String gameStatus;      // SCHEDULED, LIVE, FINISHED
    private Long timestamp;
}

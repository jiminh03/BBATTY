package com.ssafy.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class GameInfo {
    private Long gameId;
    private Long awayTeamId;
    private Long homeTeamId;
    private String awayTeamName;
    private String homeTeamName;
    private LocalDateTime dateTime;
    private String stadium;
}

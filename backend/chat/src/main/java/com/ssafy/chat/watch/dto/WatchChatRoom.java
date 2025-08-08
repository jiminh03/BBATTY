package com.ssafy.chat.watch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchChatRoom {
    private String roomId;
    private Long gameId;
    private String roomName;
    private String chatType;
    private Long teamId;

    // 경기 정보
    private String gameDate;
    private String stadium;
    private Long homeTeamId;
    private String homeTeamName;
    private Long awayTeamId;
    private String awayTeamName;

    // 참여 정보
    private int currentParticipants;
    private int maxParticipants;

    // 상태 정보
    private String status;
    private String createdAt;
}

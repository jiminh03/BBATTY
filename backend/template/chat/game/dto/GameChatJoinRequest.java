package com.ssafy.bbatty.domain.chat.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 경기 채팅 입장 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameChatJoinRequest {

    /** 팀 ID */
    @NotNull(message = "팀 ID는 필수입니다.")
    private String teamId;

    /** 경기 ID */
    @NotNull(message = "경기 ID는 필수입니다.")
    private Long gameId;
}
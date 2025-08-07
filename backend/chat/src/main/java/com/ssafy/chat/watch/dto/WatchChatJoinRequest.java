package com.ssafy.chat.watch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * 직관(Watch) 채팅방 입장 요청 DTO
 * JWT에서 teamId 추출, 완전 무명 채팅 (닉네임 없음)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class WatchChatJoinRequest {
    
    // 직관 인증 여부 (클라이언트에서 제공)
    @JsonProperty("isAttendanceVerified")
    private boolean isAttendanceVerified;
    
    // gameId 추가 (어떤 경기의 채팅방인지 구분)
    @NotNull(message = "게임 ID는 필수입니다.")
    @Min(value = 1, message = "게임 ID는 1 이상이어야 합니다.")
    private Long gameId;

    // teamId 추가 (경기의 무슨 팀인지 구분)
    @NotNull(message = "팀 ID는 필수입니다.")
    @Min(value = 1, message = "팀 ID는 1 이상이어야 합니다.")
    private Long teamId;
}
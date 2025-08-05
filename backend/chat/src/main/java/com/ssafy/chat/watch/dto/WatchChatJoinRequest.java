package com.ssafy.chat.watch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 직관(Watch) 채팅방 입장 요청 DTO
 * JWT에서 teamId 추출, 완전 무명 채팅 (닉네임 없음)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchChatJoinRequest {
    
    // 직관 인증 여부 (클라이언트에서 제공)
    @JsonProperty("isAttendanceVerified")
    private boolean isAttendanceVerified;
    
    // gameId 추가 (어떤 경기의 채팅방인지 구분)
    @NotBlank(message = "게임 ID는 필수입니다.")
    private String gameId;
}
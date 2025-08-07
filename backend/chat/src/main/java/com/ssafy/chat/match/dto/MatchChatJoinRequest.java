package com.ssafy.chat.match.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * 매칭 채팅방 입장 요청 DTO
 * JWT는 Authorization 헤더로 별도 전송 (userId, 팀정보, 성별, 나이 포함)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatJoinRequest {
    
    // 매칭 채팅방 ID 
    @NotNull(message = "매칭 ID는 필수입니다.")
    @Min(value = 1, message = "매칭 ID는 1 이상이어야 합니다.")
    private Long matchId;
    
    // 클라이언트에서 제공하는 정보
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;
    
    // 승률 정보 (정수 %)
    private int winRate;
    
    // 프로필 이미지 URL
    private String profileImgUrl;
    
    // 승요정 여부
    @JsonProperty("isWinFairy")
    private boolean isWinFairy;
}
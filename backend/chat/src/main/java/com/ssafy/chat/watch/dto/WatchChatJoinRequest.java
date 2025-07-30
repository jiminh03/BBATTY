package com.ssafy.chat.watch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 직관(Watch) 채팅방 입장 요청 DTO
 * JWT는 Authorization 헤더로 별도 전송
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchChatJoinRequest {
    
    // 클라이언트에서 제공하는 기본 데이터
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;
    
    private String profileImageUrl; // S3 링크
    
    // 직관 인증 정보
    private Boolean attendanceAuth;
    
    // 승률 정보 (자연수 %) -> 승리요정 뱃지 구분 기준
    private Integer winRate;
    
    // 직관 채팅방 ID (팀별로 분리된 채팅방)
    @NotBlank(message = "채팅방 ID는 필수입니다.")
    private String watchChatRoomId; // 예: "watch_game_001_teamA"
}
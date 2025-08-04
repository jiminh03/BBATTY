package com.ssafy.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 매칭 채팅방 입장 요청 DTO
 * JWT는 Authorization 헤더로 별도 전송 (userId, 팀정보, 성별, 나이 포함)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatJoinRequest {
    
    // 클라이언트에서 제공하는 기본 데이터
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

//    @NotBlank(message = "프로필 이미지는 필수입니다.")
//    private String profileImageUrl; // S3 링크
    
    // 승률 정보 (자연수 %)
    private Integer winRate;

    // 승리 요정 여부
//    private boolean winFairy;
    
    // 매칭 채팅방 ID 
    @NotBlank(message = "채팅방 ID는 필수입니다.")
    private String matchChatRoomId;
}
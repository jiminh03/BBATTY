package com.ssafy.chat.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 DTO
 * 인증된 사용자의 기본 정보를 담는 타입 안전한 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 사용자 닉네임
     */
    private String nickname;
    
    /**
     * 팀 ID
     */
    private Long teamId;
    
    /**
     * 팀명
     */
    private String teamName;
    
    /**
     * 사용자 나이
     */
    private Integer age;
    
    /**
     * 성별
     */
    private String gender;
    
    /**
     * 프로필 이미지 URL
     */
    private String profileImgUrl;
    
    /**
     * 승률 (매칭 채팅에서 사용)
     */
    private Double winRate;
    
    /**
     * 승리 요정 여부 (매칭 채팅에서 사용)
     */
    private Boolean isWinFairy;
    
    /**
     * 사용자 정보의 유효성 검증
     */
    public boolean isValid() {
        return userId != null && userId > 0 
               && teamId != null && teamId > 0;
    }
}
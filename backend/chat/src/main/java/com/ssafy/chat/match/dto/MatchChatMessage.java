package com.ssafy.chat.match.dto;

import com.ssafy.chat.common.dto.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class MatchChatMessage extends ChatMessage {
    // 사용자 ID (메인 서버 프로필 조회용)
    private String userId;
    
    // 사용자 닉네임
    private String nickname;
    
    // 사용자 프로필 이미지
    private String profileImgUrl;

    // 승리요정 여부
    private boolean isVictoryFairy;
}

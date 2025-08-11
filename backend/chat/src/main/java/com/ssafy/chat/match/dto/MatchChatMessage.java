package com.ssafy.chat.match.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ssafy.chat.common.dto.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchChatMessage extends ChatMessage {
    // 사용자 ID
    private Long userId;
    
    // 사용자 닉네임
    private String nickname;
    
    // 사용자 프로필 이미지
    private String profileImgUrl;

    // 승리 요정 여부
    private boolean isWinFairy;
}

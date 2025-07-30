package com.ssafy.chat.common.dto;

import com.ssafy.chat.common.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공통 채팅 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    // 메시지 타입 (CHAT vs SYSTEM)
    private MessageType messageType;

    // 채팅방 ID
    private String roomID;

    // 사용자 닉네임
    private String nickname;

    // 메시지 내용
    private String content;

}

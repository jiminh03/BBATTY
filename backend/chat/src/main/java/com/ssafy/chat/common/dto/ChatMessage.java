package com.ssafy.chat.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ssafy.chat.common.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공통 채팅 메시지 DTO (기본 구조)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    // 메시지 타입 (CHAT, SYSTEM, USER_JOIN 등)
    private MessageType messageType;

    // 채팅방 ID
    private String roomId;

    // 메시지 내용
    private String content;

    // 메시지 전송시간 (서버에서 설정)
    private Long timestamp;

}

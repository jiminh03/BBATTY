package com.ssafy.chat.watch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.chat.common.dto.ChatMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 관전 채팅 메시지 (익명 채팅 - 신고 기능을 위해 실제 userId 포함)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class WatchChatMessage extends ChatMessage {
    // 실제 사용자 ID (신고 기능 등을 위해 포함하지만 클라이언트에서는 익명 처리)
    @JsonProperty("userId")
    private Long userId;
}
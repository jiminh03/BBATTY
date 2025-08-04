package com.ssafy.chat.watch.dto;

import com.ssafy.chat.common.dto.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 관전 채팅 메시지 (익명 채팅)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class WatchChatMessage extends ChatMessage {
    
    // 트래픽 모니터링용 타임스탬프
    private Long trafficTimestamp;
}
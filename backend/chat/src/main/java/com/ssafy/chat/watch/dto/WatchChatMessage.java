package com.ssafy.chat.watch.dto;

import com.ssafy.chat.common.dto.ChatMessage;
import lombok.EqualsAndHashCode;

/**
 * 경기 직관 채팅 메시지 (익명 채팅)
 */
@EqualsAndHashCode(callSuper = true)
public class WatchChatMessage extends ChatMessage {
    // 익명 채팅이므로 추가 필드 없음
}
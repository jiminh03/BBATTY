package com.ssafy.chat.watch.dto;

import com.ssafy.chat.common.dto.ChatMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 관전 채팅 메시지 (익명 채팅)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class WatchChatMessage extends ChatMessage {
    // 관전 채팅은 완전 익명이므로 추가 필드 없음
}
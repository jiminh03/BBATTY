package com.ssafy.chat.common.event;

import com.ssafy.chat.common.domain.ChatMessage;
import com.ssafy.chat.common.domain.ChatRoomId;
import com.ssafy.chat.common.domain.UserId;
import com.ssafy.chat.common.enums.ChatRoomType;
import com.ssafy.chat.common.enums.MessageType;
import lombok.Getter;

/**
 * 채팅 메시지 발송 이벤트
 */
@Getter
public class ChatMessageSentEvent extends ChatEvent {
    
    private final ChatMessage message;
    private final ChatRoomId roomId;
    private final UserId senderId;
    private final ChatRoomType roomType;
    private final MessageType messageType;
    private final String instanceId;
    private final boolean isDistributed; // 분산 브로드캐스트 여부
    
    public ChatMessageSentEvent(Object source, ChatMessage message, ChatRoomType roomType,
                               String instanceId, boolean isDistributed) {
        super(source, "CHAT_MESSAGE_SENT");
        this.message = message;
        this.roomId = message.getRoomId();
        this.senderId = message.getSenderId();
        this.roomType = roomType;
        this.messageType = message.getMessageType();
        this.instanceId = instanceId;
        this.isDistributed = isDistributed;
    }
}
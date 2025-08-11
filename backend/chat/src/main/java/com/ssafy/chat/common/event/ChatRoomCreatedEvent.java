package com.ssafy.chat.common.event;

import com.ssafy.chat.common.domain.ChatRoom;
import com.ssafy.chat.common.domain.ChatRoomId;
import com.ssafy.chat.common.domain.UserId;
import com.ssafy.chat.common.enums.ChatRoomType;
import lombok.Getter;

/**
 * 채팅방 생성 이벤트
 */
@Getter
public class ChatRoomCreatedEvent extends ChatEvent {
    
    private final ChatRoom chatRoom;
    private final ChatRoomId roomId;
    private final ChatRoomType roomType;
    private final Long gameId;
    private final UserId creatorId;
    private final String instanceId;
    
    public ChatRoomCreatedEvent(Object source, ChatRoom chatRoom, UserId creatorId, String instanceId) {
        super(source, "CHAT_ROOM_CREATED");
        this.chatRoom = chatRoom;
        this.roomId = chatRoom.getId();
        this.roomType = chatRoom.getType();
        this.gameId = chatRoom.getGameId();
        this.creatorId = creatorId;
        this.instanceId = instanceId;
    }
}
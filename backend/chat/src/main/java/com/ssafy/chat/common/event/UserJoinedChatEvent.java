package com.ssafy.chat.common.event;

import com.ssafy.chat.common.domain.ChatRoomId;
import com.ssafy.chat.common.domain.UserId;
import com.ssafy.chat.common.dto.UserInfo;
import com.ssafy.chat.common.enums.ChatRoomType;
import lombok.Getter;

/**
 * 사용자 채팅방 입장 이벤트
 */
@Getter
public class UserJoinedChatEvent extends ChatEvent {
    
    private final ChatRoomId roomId;
    private final UserId userId;
    private final UserInfo userInfo;
    private final String sessionId;
    private final ChatRoomType roomType;
    private final String instanceId;
    
    public UserJoinedChatEvent(Object source, ChatRoomId roomId, UserId userId, 
                              UserInfo userInfo, String sessionId, 
                              ChatRoomType roomType, String instanceId) {
        super(source, "USER_JOINED_CHAT");
        this.roomId = roomId;
        this.userId = userId;
        this.userInfo = userInfo;
        this.sessionId = sessionId;
        this.roomType = roomType;
        this.instanceId = instanceId;
    }
}
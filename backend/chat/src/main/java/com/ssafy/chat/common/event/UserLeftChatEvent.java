package com.ssafy.chat.common.event;

import com.ssafy.chat.common.domain.ChatRoomId;
import com.ssafy.chat.common.domain.UserId;
import com.ssafy.chat.common.dto.UserInfo;
import com.ssafy.chat.common.enums.ChatRoomType;
import lombok.Getter;

/**
 * 사용자 채팅방 퇴장 이벤트
 */
@Getter
public class UserLeftChatEvent extends ChatEvent {
    
    private final ChatRoomId roomId;
    private final UserId userId;
    private final UserInfo userInfo;
    private final String sessionId;
    private final ChatRoomType roomType;
    private final String instanceId;
    private final String leaveReason; // 정상 퇴장, 연결 끊김, 강제 퇴장 등
    
    public UserLeftChatEvent(Object source, ChatRoomId roomId, UserId userId,
                            UserInfo userInfo, String sessionId,
                            ChatRoomType roomType, String instanceId, String leaveReason) {
        super(source, "USER_LEFT_CHAT");
        this.roomId = roomId;
        this.userId = userId;
        this.userInfo = userInfo;
        this.sessionId = sessionId;
        this.roomType = roomType;
        this.instanceId = instanceId;
        this.leaveReason = leaveReason;
    }
    
    // 편의 생성자들
    public static UserLeftChatEvent normalLeave(Object source, ChatRoomId roomId, UserId userId,
                                              UserInfo userInfo, String sessionId,
                                              ChatRoomType roomType, String instanceId) {
        return new UserLeftChatEvent(source, roomId, userId, userInfo, sessionId, roomType, instanceId, "NORMAL_LEAVE");
    }
    
    public static UserLeftChatEvent connectionLost(Object source, ChatRoomId roomId, UserId userId,
                                                 UserInfo userInfo, String sessionId,
                                                 ChatRoomType roomType, String instanceId) {
        return new UserLeftChatEvent(source, roomId, userId, userInfo, sessionId, roomType, instanceId, "CONNECTION_LOST");
    }
    
    public static UserLeftChatEvent forcedLeave(Object source, ChatRoomId roomId, UserId userId,
                                              UserInfo userInfo, String sessionId,
                                              ChatRoomType roomType, String instanceId) {
        return new UserLeftChatEvent(source, roomId, userId, userInfo, sessionId, roomType, instanceId, "FORCED_LEAVE");
    }
}
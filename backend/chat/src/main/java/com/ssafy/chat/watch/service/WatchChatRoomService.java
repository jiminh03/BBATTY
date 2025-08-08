package com.ssafy.chat.watch.service;

import com.ssafy.chat.watch.dto.ChatRoomCreateEventDto;

import java.util.List;

public interface WatchChatRoomService {

    /**
     * 관전 채팅방 자동 생성 (이벤트 기반)
     */
    String createWatchChatRoom(Long gameId, String roomName, String chatType,
                               Long teamId, ChatRoomCreateEventDto eventDto);

}

package com.ssafy.bbatty.domain.chat.common.service;

import com.ssafy.bbatty.domain.chat.common.dto.ChatRoomStatus;
import com.ssafy.bbatty.domain.chat.common.enums.ChatRoomType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 채팅방 관리를 위한 기본 서비스 인터페이스
 */
public interface BaseChatRoomService {

    /**
     * 채팅방 존재 여부 확인
     */
    boolean existsChatRoom(String roomId);

    /**
     * 채팅방 활성화 상태 확인
     */
    boolean isRoomActive(String roomId);

    /**
     * 채팅방 상태 조회
     */
    ChatRoomStatus getRoomStatus(String roomId);

    /**
     * 채팅방 TTL 설정
     */
    boolean setRoomTTL(String roomId, long ttlSeconds);
}
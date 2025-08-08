package com.ssafy.chat.common.service;

import com.ssafy.chat.common.infrastructure.ChatRoomRedisManager;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 채팅방 관리 서비스 구현체
 * 비즈니스 로직을 처리하고 인프라 레이어에 위임
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRedisManager chatRoomRedisManager;

    @Override
    public void enterRoom(String roomId, String sessionId, Map<String, Object> userInfo) {
        try {
            // 이미 다른 채팅방에 입장되어 있다면 먼저 퇴장 처리
            String currentRoom = chatRoomRedisManager.findRoomBySession(sessionId);
            if (currentRoom != null && !currentRoom.equals(roomId)) {
                log.info("사용자가 다른 채팅방에서 이동 - 기존: {}, 새로운: {}, sessionId: {}", 
                        currentRoom, roomId, sessionId);
                chatRoomRedisManager.removeUserFromRoom(currentRoom, sessionId);
            }
            
            // 새 채팅방에 입장
            chatRoomRedisManager.addUserToRoom(roomId, sessionId, userInfo);
            
            log.info("채팅방 입장 완료 - roomId: {}, sessionId: {}, userCount: {}", 
                    roomId, sessionId, getRoomUserCount(roomId));
                    
        } catch (Exception e) {
            log.error("채팅방 입장 처리 실패 - roomId: {}, sessionId: {}", roomId, sessionId, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }

    @Override
    public void leaveRoom(String sessionId) {
        try {
            String roomId = chatRoomRedisManager.findRoomBySession(sessionId);
            if (roomId != null) {
                chatRoomRedisManager.removeUserFromRoom(roomId, sessionId);
                log.info("채팅방 퇴장 완료 - roomId: {}, sessionId: {}, remainingUsers: {}", 
                        roomId, sessionId, getRoomUserCount(roomId));
            } else {
                log.debug("퇴장할 채팅방을 찾을 수 없음 - sessionId: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("채팅방 퇴장 처리 실패 - sessionId: {}", sessionId, e);
        }
    }

    @Override
    public List<Map<String, Object>> getRoomUsers(String roomId) {
        try {
            return chatRoomRedisManager.getRoomUsers(roomId);
        } catch (Exception e) {
            log.error("채팅방 사용자 목록 조회 실패 - roomId: {}", roomId, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }

    @Override
    public int getRoomUserCount(String roomId) {
        try {
            return chatRoomRedisManager.getRoomUserCount(roomId);
        } catch (Exception e) {
            log.error("채팅방 사용자 수 조회 실패 - roomId: {}", roomId, e);
            return 0;
        }
    }

    @Override
    public String getRoomBySession(String sessionId) {
        try {
            return chatRoomRedisManager.findRoomBySession(sessionId);
        } catch (Exception e) {
            log.error("세션별 채팅방 조회 실패 - sessionId: {}", sessionId, e);
            return null;
        }
    }

    @Override
    public void createRoom(String roomId, String roomType, Map<String, Object> additionalInfo) {
        try {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", roomId);
            roomInfo.put("roomType", roomType);
            roomInfo.put("createdAt", System.currentTimeMillis());
            roomInfo.put("userCount", 0);
            
            // 추가 정보 병합
            if (additionalInfo != null) {
                roomInfo.putAll(additionalInfo);
            }
            
            chatRoomRedisManager.saveRoomInfo(roomId, roomInfo);
            
            log.info("채팅방 생성 완료 - roomId: {}, roomType: {}", roomId, roomType);
            
        } catch (Exception e) {
            log.error("채팅방 생성 실패 - roomId: {}, roomType: {}", roomId, roomType, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }
}
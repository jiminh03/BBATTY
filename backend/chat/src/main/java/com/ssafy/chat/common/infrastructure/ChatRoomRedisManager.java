package com.ssafy.chat.common.infrastructure;

import com.ssafy.chat.common.util.ChatRoomTTLManager;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.global.constants.ChatRedisKey;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis 기반 채팅방 관리 인프라 컴포넌트
 * 실제 Redis 조작을 담당
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomRedisManager {

    private final RedisUtil redisUtil;

    // Redis Key 패턴은 ChatRedisKey 클래스에서 관리

    /**
     * 채팅방에 사용자 추가
     */
    public void addUserToRoom(String roomId, String sessionId, Map<String, Object> userInfo) {
        try {
            String roomUsersKey = ChatRedisKey.getChatRoomUsersKey(roomId);
            String sessionRoomKey = ChatRedisKey.getSessionRoomMappingKey(sessionId);
            
            Duration expireTime = ChatRoomTTLManager.getTTLUntilMidnight();
            
            // 채팅방 사용자 목록에 추가
            redisUtil.setHashValue(roomUsersKey, sessionId, userInfo, expireTime);
            
            // 세션별 채팅방 매핑 저장
            redisUtil.setValue(sessionRoomKey, roomId, expireTime);
            
            log.info("사용자 채팅방 입장 - roomId: {}, sessionId: {}, userId: {}, 만료시간: {}분", 
                    roomId, sessionId, userInfo.get("userId"), ChatRoomTTLManager.toMinutes(expireTime));
            
        } catch (Exception e) {
            log.error("채팅방 사용자 추가 실패 - roomId: {}, sessionId: {}", roomId, sessionId, e);
            throw new ApiException(ErrorCode.SERVER_ERROR);
        }
    }

    /**
     * 채팅방에서 사용자 제거
     */
    public void removeUserFromRoom(String roomId, String sessionId) {
        try {
            String roomUsersKey = ChatRedisKey.getChatRoomUsersKey(roomId);
            String sessionRoomKey = ChatRedisKey.getSessionRoomMappingKey(sessionId);
            
            // 채팅방 사용자 목록에서 제거
            redisUtil.deleteHashKey(roomUsersKey, sessionId);
            
            // 세션별 채팅방 매핑 제거
            redisUtil.deleteKey(sessionRoomKey);
            
            log.info("사용자 채팅방 퇴장 - roomId: {}, sessionId: {}", roomId, sessionId);
            
            // 채팅방이 비어있으면 정리
            if (getRoomUserCount(roomId) == 0) {
                cleanupEmptyRoom(roomId);
            }
            
        } catch (Exception e) {
            log.error("채팅방 사용자 제거 실패 - roomId: {}, sessionId: {}", roomId, sessionId, e);
        }
    }

    /**
     * 채팅방 사용자 목록 조회
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRoomUsers(String roomId) {
        try {
            String roomUsersKey = ChatRedisKey.getChatRoomUsersKey(roomId);
            Map<String, Object> usersHash = redisUtil.getHashEntries(roomUsersKey);
            
            return usersHash.values().stream()
                    .map(user -> (Map<String, Object>) user)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("채팅방 사용자 목록 조회 실패 - roomId: {}", roomId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 채팅방 사용자 수 조회
     */
    public int getRoomUserCount(String roomId) {
        try {
            String roomUsersKey = ChatRedisKey.getChatRoomUsersKey(roomId);
            return redisUtil.getHashSize(roomUsersKey);
        } catch (Exception e) {
            log.error("채팅방 사용자 수 조회 실패 - roomId: {}", roomId, e);
            return 0;
        }
    }

    /**
     * 세션으로 채팅방 찾기
     */
    public String findRoomBySession(String sessionId) {
        try {
            String sessionRoomKey = ChatRedisKey.getSessionRoomMappingKey(sessionId);
            Object roomId = redisUtil.getValue(sessionRoomKey);
            return roomId != null ? roomId.toString() : null;
        } catch (Exception e) {
            log.error("세션별 채팅방 조회 실패 - sessionId: {}", sessionId, e);
            return null;
        }
    }

    /**
     * 채팅방 정보 저장
     */
    public void saveRoomInfo(String roomId, Map<String, Object> roomInfo) {
        try {
            String roomInfoKey = ChatRedisKey.getChatRoomInfoKey(roomId);
            Duration expireTime = ChatRoomTTLManager.getTTLUntilMidnight();
            redisUtil.setValue(roomInfoKey, roomInfo, expireTime);
            log.debug("채팅방 정보 저장 - roomId: {}, 만료시간: {}분", roomId, ChatRoomTTLManager.toMinutes(expireTime));
        } catch (Exception e) {
            log.error("채팅방 정보 저장 실패 - roomId: {}", roomId, e);
        }
    }

    /**
     * 채팅방 정보 조회
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRoomInfo(String roomId) {
        try {
            String roomInfoKey = ChatRedisKey.getChatRoomInfoKey(roomId);
            Object roomInfo = redisUtil.getValue(roomInfoKey);
            return roomInfo != null ? (Map<String, Object>) roomInfo : new HashMap<>();
        } catch (Exception e) {
            log.error("채팅방 정보 조회 실패 - roomId: {}", roomId, e);
            return new HashMap<>();
        }
    }

    // TTL 계산은 ChatRoomTTLManager에서 처리

    /**
     * 빈 채팅방 정리
     */
    private void cleanupEmptyRoom(String roomId) {
        try {
            String roomUsersKey = ChatRedisKey.getChatRoomUsersKey(roomId);
            String roomInfoKey = ChatRedisKey.getChatRoomInfoKey(roomId);
            
            redisUtil.deleteKey(roomUsersKey);
            redisUtil.deleteKey(roomInfoKey);
            
            log.info("빈 채팅방 정리 완료 - roomId: {}", roomId);
        } catch (Exception e) {
            log.error("빈 채팅방 정리 실패 - roomId: {}", roomId, e);
        }
    }

    /**
     * 채팅방 강제 삭제 (관리자용)
     */
    public void forceDeleteRoom(String roomId) {
        try {
            String roomUsersKey = ChatRedisKey.getChatRoomUsersKey(roomId);
            String roomInfoKey = ChatRedisKey.getChatRoomInfoKey(roomId);
            
            // 모든 세션의 채팅방 매핑도 제거
            Map<String, Object> users = redisUtil.getHashEntries(roomUsersKey);
            for (String sessionId : users.keySet()) {
                String sessionRoomKey = ChatRedisKey.getSessionRoomMappingKey(sessionId);
                redisUtil.deleteKey(sessionRoomKey);
            }
            
            redisUtil.deleteKey(roomUsersKey);
            redisUtil.deleteKey(roomInfoKey);
            
            log.info("채팅방 강제 삭제 완료 - roomId: {}", roomId);
        } catch (Exception e) {
            log.error("채팅방 강제 삭제 실패 - roomId: {}", roomId, e);
        }
    }
}
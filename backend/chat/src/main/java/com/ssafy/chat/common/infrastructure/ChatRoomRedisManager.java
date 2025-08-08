package com.ssafy.chat.common.infrastructure;

import com.ssafy.chat.common.util.RedisUtil;
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

    // Redis Key 패턴
    private static final String ROOM_USERS_KEY = "chat_room:users:%s";        // 채팅방별 사용자 목록
    private static final String SESSION_ROOM_KEY = "chat_session:room:%s";    // 세션별 채팅방 매핑
    private static final String ROOM_INFO_KEY = "chat_room:info:%s";          // 채팅방 정보

    /**
     * 채팅방에 사용자 추가
     */
    public void addUserToRoom(String roomId, String sessionId, Map<String, Object> userInfo) {
        try {
            String roomUsersKey = String.format(ROOM_USERS_KEY, roomId);
            String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
            
            Duration expireTime = getExpireTimeUntilMidnight();
            
            // 채팅방 사용자 목록에 추가
            redisUtil.setHashValue(roomUsersKey, sessionId, userInfo, expireTime);
            
            // 세션별 채팅방 매핑 저장
            redisUtil.setValue(sessionRoomKey, roomId, expireTime);
            
            log.info("사용자 채팅방 입장 - roomId: {}, sessionId: {}, userId: {}, 만료시간: {}분", 
                    roomId, sessionId, userInfo.get("userId"), expireTime.toMinutes());
            
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
            String roomUsersKey = String.format(ROOM_USERS_KEY, roomId);
            String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
            
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
            String roomUsersKey = String.format(ROOM_USERS_KEY, roomId);
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
            String roomUsersKey = String.format(ROOM_USERS_KEY, roomId);
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
            String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
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
            String roomInfoKey = String.format(ROOM_INFO_KEY, roomId);
            Duration expireTime = getExpireTimeUntilMidnight();
            redisUtil.setValue(roomInfoKey, roomInfo, expireTime);
            log.debug("채팅방 정보 저장 - roomId: {}, 만료시간: {}분", roomId, expireTime.toMinutes());
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
            String roomInfoKey = String.format(ROOM_INFO_KEY, roomId);
            Object roomInfo = redisUtil.getValue(roomInfoKey);
            return roomInfo != null ? (Map<String, Object>) roomInfo : new HashMap<>();
        } catch (Exception e) {
            log.error("채팅방 정보 조회 실패 - roomId: {}", roomId, e);
            return new HashMap<>();
        }
    }

    /**
     * 당일 자정까지의 만료 시간 계산
     * 매칭 채팅방과 직관 채팅방 모두 당일 자정에 만료
     */
    private Duration getExpireTimeUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration duration = Duration.between(now, midnight);
        
        // 최소 1분은 보장 (자정 직전에 입장하는 경우 대비)
        return duration.isZero() || duration.isNegative() ? Duration.ofMinutes(1) : duration;
    }

    /**
     * 빈 채팅방 정리
     */
    private void cleanupEmptyRoom(String roomId) {
        try {
            String roomUsersKey = String.format(ROOM_USERS_KEY, roomId);
            String roomInfoKey = String.format(ROOM_INFO_KEY, roomId);
            
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
            String roomUsersKey = String.format(ROOM_USERS_KEY, roomId);
            String roomInfoKey = String.format(ROOM_INFO_KEY, roomId);
            
            // 모든 세션의 채팅방 매핑도 제거
            Map<String, Object> users = redisUtil.getHashEntries(roomUsersKey);
            for (String sessionId : users.keySet()) {
                String sessionRoomKey = String.format(SESSION_ROOM_KEY, sessionId);
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
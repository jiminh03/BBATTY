package com.ssafy.chat.watch.service;

import com.ssafy.chat.common.util.KSTTimeUtil;

import com.ssafy.chat.common.util.ChatRoomTTLManager;
import com.ssafy.chat.common.util.JsonUtils;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.global.constants.ChatRedisKey;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.watch.dto.WatchChatMessage;
import com.ssafy.chat.watch.redis.WatchChatRedisPub;
import com.ssafy.chat.watch.redis.WatchChatRedisSub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 관전 채팅 서비스 구현체
 * Redis Pub/Sub 인프라와 연동하여 관전 채팅 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WatchChatServiceImpl implements WatchChatService {
    
    private final WatchChatRedisPub redisPub;
    private final WatchChatRedisSub redisSub;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisUtil redisUtil;
    
    private static final int TRAFFIC_SPIKE_THRESHOLD = 100; // 최근 3분간 100개 이상
    private static final int TRAFFIC_WINDOW_MINUTES = 3;
    
    @Override
    public void addSessionToWatchRoom(String roomId, WebSocketSession session) {
        log.debug("관전 채팅방에 세션 추가 - roomId: {}, sessionId: {}", roomId, session.getId());
        redisSub.addSessionToWatchChatRoom(roomId, session);
    }
    
    @Override
    public void removeSessionFromWatchRoom(String roomId, WebSocketSession session) {
        log.debug("관전 채팅방에서 세션 제거 - roomId: {}, sessionId: {}", roomId, session.getId());
        redisSub.removeSessionFromWatchChatRoom(roomId, session);
    }
    
    @Override
    public void sendChatMessage(String roomId, WatchChatMessage message) {
        log.debug("관전 채팅 메시지 발송 - roomId: {}", roomId);
        
        // 메시지를 Map으로 변환하여 발송
        Map<String, Object> messageMap = createMessageMap(message);
        redisPub.publishMessage(roomId, messageMap);
    }
    
    @Override
    public void sendUserJoinEvent(String roomId, String userId, String userName) {
        log.debug("관전 채팅 사용자 입장 이벤트 발송 - roomId: {}, userId: {}", roomId, userId);
        redisPub.publishUserJoinEvent(roomId, userId, userName);
    }
    
    @Override
    public void sendUserLeaveEvent(String roomId, String userId, String userName) {
        log.debug("관전 채팅 사용자 퇴장 이벤트 발송 - roomId: {}, userId: {}", roomId, userId);
        redisPub.publishUserLeaveEvent(roomId, userId, userName);
    }
    
    @Override
    public void incrementTrafficCount(String roomId) {
        try {
            String currentMinute = KSTTimeUtil.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            String trafficKey = ChatRedisKey.getWatchTrafficKey(roomId, currentMinute);
            
            // 현재 분의 메시지 수 증가
            redisTemplate.opsForValue().increment(trafficKey);
            redisTemplate.expire(trafficKey, ChatRoomTTLManager.getTrafficMonitoringTTL());
            
            log.debug("관전 채팅 트래픽 카운트 증가 - roomId: {}, key: {}", roomId, trafficKey);
            
        } catch (Exception e) {
            log.warn("관전 채팅 트래픽 카운트 증가 실패 - roomId: {}", roomId, e);
        }
    }
    
    @Override
    public void checkTrafficSpike(String roomId) {
        try {
            LocalDateTime nowTime = KSTTimeUtil.now();
            long totalMessages = 0;
            
            // 최근 N분간의 메시지 수 합계
            for (int i = 0; i < TRAFFIC_WINDOW_MINUTES; i++) {
                String minute = now.minusMinutes(i)
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
                String key = ChatRedisKey.getWatchTrafficKey(roomId, minute);
                
                Object count = redisTemplate.opsForValue().get(key);
                if (count != null) {
                    totalMessages += Long.parseLong(count.toString());
                }
            }
            
            // 임계값 초과 시 로그 출력
            if (totalMessages > TRAFFIC_SPIKE_THRESHOLD) {
                log.warn("관전 채팅 트래픽 급증 감지 - roomId: {}, 최근 {}분간 메시지: {}개", 
                        roomId, TRAFFIC_WINDOW_MINUTES, totalMessages);
                
                // 필요하다면 여기서 알림이나 추가 처리 가능
                // 예: 관리자 알림, 레이트 리미팅 등
            }
            
        } catch (Exception e) {
            log.warn("관전 채팅 트래픽 급증 감지 실패 - roomId: {}", roomId, e);
        }
    }
    
    @Override
    public int getActiveWatchRoomCount() {
        return redisSub.getActiveWatchRoomCount();
    }
    
    @Override
    public int getActiveSessionCount(String roomId) {
        return redisSub.getActiveSessionCount(roomId);
    }
    
    @Override
    public boolean validateUserSession(String sessionToken) {
        try {
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                return false;
            }
            
            String sessionKey = ChatRedisKey.getUserSessionKey(sessionToken);
            return redisTemplate.hasKey(sessionKey);
            
        } catch (Exception e) {
            log.error("세션 토큰 검증 실패 - token: {}", sessionToken, e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getUserInfoFromSession(String sessionToken) {
        try {
            String sessionKey = ChatRedisKey.getUserSessionKey(sessionToken);
            Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
            
            if (sessionData.isEmpty()) {
                log.warn("세션 정보 없음 - token: {}", sessionToken);
                return Map.of();
            }
            
            // Object -> String 변환
            Map<String, Object> userInfo = new HashMap<>();
            sessionData.forEach((key, value) -> userInfo.put(key.toString(), value));
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("세션에서 사용자 정보 조회 실패 - token: {}", sessionToken, e);
            return Map.of();
        }
    }
    
    /**
     * WatchChatMessage를 Map으로 변환
     */
    private Map<String, Object> createMessageMap(WatchChatMessage message) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageType", message.getMessageType());
        messageMap.put("roomId", message.getRoomId());
        messageMap.put("content", message.getContent());
        messageMap.put("timestamp", message.getTimestamp().toString());
        messageMap.put("userId", message.getUserId()); // userId 추가!
        
        return messageMap;
    }
    
    @Override
    public void createWatchChatRoom(String roomId, Long gameId, Long teamId, String teamName) {
        try {
            // Redis에 직관 채팅방 정보 저장
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", roomId);
            roomInfo.put("gameId", gameId);
            roomInfo.put("teamId", teamId);
            roomInfo.put("teamName", teamName);
            roomInfo.put("roomType", "WATCH");
            roomInfo.put("createdAt", System.currentTimeMillis());
            roomInfo.put("status", "ACTIVE");
            
            // Redis에 저장
            String redisKey = ChatRedisKey.getWatchRoomInfoKey(roomId);
            redisTemplate.opsForHash().putAll(redisKey, roomInfo);
            
            // TTL 설정 (7일)
            redisTemplate.expire(redisKey, ChatRoomTTLManager.getTTLUntilMidnight());
            
            log.info("직관 채팅방 생성됨: roomId={}, gameId={}, teamId={}, teamName={}", 
                    roomId, gameId, teamId, teamName);
                    
        } catch (Exception e) {
            log.error("직관 채팅방 생성 실패: roomId={}", roomId, e);
            throw new ApiException(ErrorCode.WATCH_CHAT_ROOM_CREATE_FAILED);
        }
    }
    
    @Override
    public Map<String, Object> getWatchChatRooms() {
        try {
            log.debug("관전 채팅방 목록 조회 시작");
            
            // Redis에서 모든 관전 채팅방 조회 (SCAN 사용으로 성능 최적화)
            String pattern = ChatRedisKey.WATCH_ROOM_INFO + "*";
            var keys = redisUtil.scanKeys(pattern);
            
            Map<String, Object> result = new HashMap<>();
            result.put("rooms", new HashMap<>());
            result.put("totalCount", 0);
            
            if (keys == null || keys.isEmpty()) {
                log.info("활성화된 관전 채팅방이 없습니다");
                return result;
            }
            
            Map<String, Object> rooms = new HashMap<>();
            int successCount = 0;
            
            for (String key : keys) {
                try {
                    Map<Object, Object> roomData = redisTemplate.opsForHash().entries(key);
                    if (roomData.isEmpty()) {
                        log.warn("빈 채팅방 데이터: key={}", key);
                        continue;
                    }
                    
                    Object roomIdObj = roomData.get("roomId");
                    if (roomIdObj == null) {
                        log.warn("roomId가 없는 채팅방 데이터: key={}", key);
                        continue;
                    }
                    
                    String roomId = roomIdObj.toString();
                    
                    Map<String, Object> roomInfo = new HashMap<>();
                    roomData.forEach((k, v) -> {
                        if (k != null && v != null) {
                            roomInfo.put(k.toString(), v);
                        }
                    });
                    
                    // 현재 활성 세션 수 추가
                    try {
                        int sessionCount = getActiveSessionCount(roomId);
                        roomInfo.put("currentUsers", sessionCount);
                    } catch (Exception e) {
                        log.warn("세션 수 조회 실패: roomId={}", roomId, e);
                        roomInfo.put("currentUsers", 0);
                    }
                    
                    rooms.put(roomId, roomInfo);
                    successCount++;
                    
                } catch (Exception e) {
                    log.warn("채팅방 정보 처리 실패: key={}", key, e);
                }
            }
            
            result.put("rooms", rooms);
            result.put("totalCount", successCount);
            
            log.info("관전 채팅방 목록 조회 완료: 총 {}개", successCount);
            return result;
            
        } catch (Exception e) {
            log.error("관전 채팅방 목록 조회 중 오류 발생", e);
            throw new ApiException(ErrorCode.WATCH_CHAT_ROOM_LIST_FAILED);
        }
    }
    
    @Override
    public Map<String, Object> getWatchChatRoom(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            log.warn("유효하지 않은 roomId: {}", roomId);
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        try {
            log.debug("관전 채팅방 조회 시작: roomId={}", roomId);
            
            String redisKey = ChatRedisKey.getWatchRoomInfoKey(roomId);
            Map<Object, Object> roomData = redisTemplate.opsForHash().entries(redisKey);
            
            if (roomData == null || roomData.isEmpty()) {
                log.warn("관전 채팅방을 찾을 수 없습니다: roomId={}", roomId);
                throw new ApiException(ErrorCode.WATCH_CHAT_ROOM_NOT_FOUND);
            }
            
            // 필수 필드 검증
            Object roomIdObj = roomData.get("roomId");
            Object gameIdObj = roomData.get("gameId");
            Object teamIdObj = roomData.get("teamId");
            
            if (roomIdObj == null || gameIdObj == null || teamIdObj == null) {
                log.error("관전 채팅방 필수 정보 누락: roomId={}, roomData={}", roomId, roomData);
                throw new ApiException(ErrorCode.WATCH_CHAT_ROOM_INFO_INVALID);
            }
            
            Map<String, Object> roomInfo = new HashMap<>();
            roomData.forEach((k, v) -> {
                if (k != null && v != null) {
                    roomInfo.put(k.toString(), v);
                }
            });
            
            // 현재 활성 세션 수 추가
            try {
                int sessionCount = getActiveSessionCount(roomId);
                roomInfo.put("currentUsers", sessionCount);
            } catch (Exception e) {
                log.warn("세션 수 조회 실패: roomId={}", roomId, e);
                roomInfo.put("currentUsers", 0);
            }
            
            log.debug("관전 채팅방 조회 완료: roomId={}", roomId);
            return roomInfo;
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("관전 채팅방 조회 중 오류 발생: roomId={}", roomId, e);
            throw new ApiException(ErrorCode.REDIS_OPERATION_FAILED);
        }
    }
}
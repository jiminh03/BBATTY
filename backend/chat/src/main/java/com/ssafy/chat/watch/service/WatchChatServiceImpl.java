package com.ssafy.chat.watch.service;

import com.ssafy.chat.common.util.JsonUtils;
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
    
    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final String TRAFFIC_KEY_PREFIX = "chat:traffic:";
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
            String currentMinute = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            String trafficKey = TRAFFIC_KEY_PREFIX + roomId + ":" + currentMinute;
            
            // 현재 분의 메시지 수 증가
            redisTemplate.opsForValue().increment(trafficKey);
            redisTemplate.expire(trafficKey, TRAFFIC_WINDOW_MINUTES + 1, TimeUnit.MINUTES);
            
            log.debug("관전 채팅 트래픽 카운트 증가 - roomId: {}, key: {}", roomId, trafficKey);
            
        } catch (Exception e) {
            log.warn("관전 채팅 트래픽 카운트 증가 실패 - roomId: {}", roomId, e);
        }
    }
    
    @Override
    public void checkTrafficSpike(String roomId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            long totalMessages = 0;
            
            // 최근 N분간의 메시지 수 합계
            for (int i = 0; i < TRAFFIC_WINDOW_MINUTES; i++) {
                String minute = now.minusMinutes(i)
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
                String key = TRAFFIC_KEY_PREFIX + roomId + ":" + minute;
                
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
            
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            return redisTemplate.hasKey(sessionKey);
            
        } catch (Exception e) {
            log.error("세션 토큰 검증 실패 - token: {}", sessionToken, e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getUserInfoFromSession(String sessionToken) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
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
        
        return messageMap;
    }
}
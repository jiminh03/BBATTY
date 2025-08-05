package com.ssafy.chat.watch.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 관전 채팅 Redis Publisher (메시지 발행)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WatchChatRedisPub {
    
    @Qualifier("redisPubSubTemplate")
    private final RedisTemplate<String, Object> redisPubSubTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 채팅방에 메시지 발행
     */
    public void publishMessage(String roomId, Map<String, Object> message) {
        try {
            // Map 객체를 직접 발행 (PubSub 전용 템플릿 사용)
            redisPubSubTemplate.convertAndSend(getChannelName(roomId), message);
            log.debug("관전 채팅 메시지 발행 - roomId: {}", roomId);
        } catch (Exception e) {
            log.error("관전 채팅 메시지 발행 실패 - roomId: {}", roomId, e);
        }
    }
    
    /**
     * 사용자 입장 이벤트 발행
     */
    public void publishUserJoinEvent(String roomId, String userId, String userName) {
        Map<String, Object> joinEvent = Map.of(
                "type", "user_join",
                "userId", userId,
                "userName", userName,
                "roomId", roomId,
                "timestamp", System.currentTimeMillis()
        );
        publishMessage(roomId, joinEvent);
    }
    
    /**
     * 사용자 퇴장 이벤트 발행
     */
    public void publishUserLeaveEvent(String roomId, String userId, String userName) {
        Map<String, Object> leaveEvent = Map.of(
                "type", "user_leave",
                "userId", userId,
                "userName", userName,
                "roomId", roomId,
                "timestamp", System.currentTimeMillis()
        );
        publishMessage(roomId, leaveEvent);
    }
    
    /**
     * 채널명 생성
     */
    private String getChannelName(String roomId) {
        return "watch-chat:" + roomId;
    }
}
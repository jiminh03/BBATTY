package com.ssafy.chat.common.service;

import java.util.Map;

/**
 * Redis Pub/Sub 서비스 인터페이스
 */
public interface RedisPubSubService {
    
    /**
     * 메시지 발행
     */
    void publishMessage(String channel, Map<String, Object> message);
    
    /**
     * 채널 구독
     */
    void subscribeToRoom(String roomId, ChatMessageHandler handler);
    
    /**
     * 채널 구독 해제
     */
    void unsubscribeFromRoom(String roomId);
    
    /**
     * 메시지 핸들러 인터페이스
     */
    @FunctionalInterface
    interface ChatMessageHandler {
        void handle(String roomId, Map<String, Object> message);
    }
}
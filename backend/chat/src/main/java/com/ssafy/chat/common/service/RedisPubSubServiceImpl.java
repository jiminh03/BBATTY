package com.ssafy.chat.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Redis Pub/Sub 서비스 구현체 (간단 버전)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPubSubServiceImpl implements RedisPubSubService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishMessage(String channel, Map<String, Object> message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend("chat:" + channel, jsonMessage);
            log.debug("메시지 발행 - channel: {}", channel);
        } catch (Exception e) {
            log.error("메시지 발행 실패 - channel: {}", channel, e);
        }
    }

    @Override
    public void subscribeToRoom(String roomId, ChatMessageHandler handler) {
        // 간단 버전에서는 구독 로직 생략
        log.debug("채널 구독 - roomId: {}", roomId);
    }

    @Override
    public void unsubscribeFromRoom(String roomId) {
        // 간단 버전에서는 구독 해제 로직 생략
        log.debug("채널 구독 해제 - roomId: {}", roomId);
    }
}
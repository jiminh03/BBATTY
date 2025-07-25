package com.ssafy.bbatty.domain.chat.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Pub/Sub 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPubSubServiceImpl implements RedisPubSubService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer messageListenerContainer;
    private final ObjectMapper objectMapper;

    // 채널별 구독 상태 관리 (채널명 -> 리스너 매핑으로 변경)
    private final Map<String, MessageListener> activeListeners = new ConcurrentHashMap<>();

    @Override
    public void publishMessage(String roomId, Map<String, Object> message) {
        try {
            String channel = createChannelName(roomId);

            log.debug("Redis 메시지 발행 - 채널: {}, 메시지: {}", channel, message);

            // Redis에 메시지 발행
            redisTemplate.convertAndSend(channel, message);

        } catch (Exception e) {
            log.error("Redis 메시지 발행 실패 - 채널: {}", roomId, e);
        }
    }

    @Override
    public void subscribeToRoom(String roomId, ChatMessageHandler messageHandler) {
        String channelName = createChannelName(roomId);

        // 이미 구독 중인 채널인지 확인
        if (activeListeners.containsKey(channelName)) {
            log.debug("이미 구독 중인 채널: {}", channelName);
            return;
        }

        try {
            ChannelTopic topic = new ChannelTopic(channelName);

            // 메시지 리스너 생성 및 등록
            ChatMessageListener listener = new ChatMessageListener(messageHandler, objectMapper);
            messageListenerContainer.addMessageListener(listener, topic);

            // 구독 상태 저장 (리스너도 함께 저장)
            activeListeners.put(channelName, listener);

            log.info("Redis 채널 구독 시작: {}", channelName);

        } catch (Exception e) {
            log.error("Redis 채널 구독 실패: {}", channelName, e);
        }
    }

    @Override
    public void unsubscribeFromRoom(String roomId) {
        String channelName = createChannelName(roomId);
        MessageListener listener = activeListeners.remove(channelName);

        if (listener != null) {
            try {
                ChannelTopic topic = new ChannelTopic(channelName);
                messageListenerContainer.removeMessageListener(listener, topic);
                log.info("Redis 채널 구독 해제: {}", channelName);

            } catch (Exception e) {
                log.error("Redis 채널 구독 해제 실패: {}", channelName, e);
            }
        } else {
            log.debug("구독하지 않은 채널 구독 해제 시도: {}", channelName);
        }
    }

    @Override
    public Map<String, ChannelTopic> getSubscribedChannels() {
        Map<String, ChannelTopic> result = new ConcurrentHashMap<>();

        activeListeners.keySet().forEach(channelName ->
                result.put(channelName, new ChannelTopic(channelName))
        );

        return result;
    }

    /**
     * 채널명 생성 규칙
     * @param roomId 채팅방 ID (예: "tigers")
     * @return Redis 채널명 (예: "chat:tigers")
     */
    private String createChannelName(String roomId) {
        return "chat:" + roomId;
    }

    /**
     * Redis 메시지 리스너 구현체
     */
    public static class ChatMessageListener implements MessageListener {

        private final ChatMessageHandler messageHandler;
        private final ObjectMapper objectMapper;

        public ChatMessageListener(ChatMessageHandler messageHandler, ObjectMapper objectMapper) {
            this.messageHandler = messageHandler;
            this.objectMapper = objectMapper;
        }

        @Override
        public void onMessage(@NonNull Message message, byte[] pattern) {
            try {
                // 채널명에서 roomId 추출
                String channel = new String(message.getChannel());
                String roomId = channel.substring(5); // "chat:" 제거

                // 메시지 본문 파싱
                String messageBody = new String(message.getBody());
                @SuppressWarnings("unchecked")
                Map<String, Object> messageData = objectMapper.readValue(messageBody, Map.class);

                log.debug("Redis 메시지 수신 - 채널: {}, 방: {}, 데이터: {}", channel, roomId, messageData);

                // 메시지 핸들러로 전달
                messageHandler.handleMessage(roomId, messageData);

            } catch (Exception e) {
                log.error("Redis 메시지 처리 오류", e);
            }
        }
    }
}
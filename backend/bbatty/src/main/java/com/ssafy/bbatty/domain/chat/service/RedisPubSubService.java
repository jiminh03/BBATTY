package com.ssafy.bbatty.domain.chat.service;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.Map;

public interface RedisPubSubService {
    /**
     * 메시지 발행 (publish)
     * @Param roomId 채팅방 ID (예: "game123_tigers")
     * @Param message 전송할 메시지 (예: Map 형태)
     */
    void publishMessage(String roomId, Map<String, Object> message);

    /**
     * 채널 구독 (Subscribe)
     * @Param roomId 채팅방 ID
     * @Param messageHandler 메시지 처리 핸들러
     */
    void subscribeToRoom(String roomId, ChatMessageHandler messageHandler);

    /**
     * 채널 구독 해제
     * @param roomId 채팅방 ID
     */
    void unsubscribeFromRoom(String roomId);

    /**
     * 현재 구독중인 채널 목록 조회 (디버깅용)
     * @return Map<String, ChannelTopic> 채널 목록
     */
    Map<String, ChannelTopic> getSubscribedChannels();

    /**
     * 채팅 메시지 처리 핸들러 인터페이스
     */
    interface ChatMessageHandler
    {
        void handleMessage(String roomId, Map<String, Object> message);
    }

}

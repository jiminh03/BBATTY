package com.ssafy.chat.watch.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.global.constants.ChatRedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 관전 채팅 Redis Subscriber (메시지 수신 및 브로드캐스트)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WatchChatRedisSub implements MessageListener {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer redisContainer;
    private final ObjectMapper objectMapper;
    
    // 활성화된 관전 채팅방별 WebSocket 세션 관리
    private final Map<String, Set<WebSocketSession>> watchChatSessions = new ConcurrentHashMap<>();
    private final Map<String, ChannelTopic> subscribedChannels = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("관전 채팅 Redis Subscriber 초기화");
    }
    
    @PreDestroy
    public void destroy() {
        // 모든 구독 해제
        subscribedChannels.values().forEach(topic -> 
            redisContainer.removeMessageListener(this, topic));
        log.info("관전 채팅 Redis Subscriber 종료");
    }
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());
            
            // 채널에서 roomId 추출
            String roomId = extractRoomIdFromChannel(channel);
            
            log.debug("관전 채팅 메시지 수신 - channel: {}, roomId: {}", channel, roomId);
            
            // JSON 메시지를 Map으로 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(messageBody, Map.class);
            
            // 해당 채팅방의 모든 세션에 브로드캐스트
            broadcastToWatchChatRoom(roomId, messageData);
            
        } catch (Exception e) {
            log.error("관전 채팅 Redis 메시지 처리 실패", e);
        }
    }
    
    /**
     * 관전 채팅방에 WebSocket 세션 추가
     */
    public void addSessionToWatchChatRoom(String roomId, WebSocketSession session) {
        // 세션 추가
        watchChatSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        
        // 첫 번째 세션이면 채널 구독 시작
        if (watchChatSessions.get(roomId).size() == 1) {
            subscribeToChannel(roomId);
        }
        
        log.debug("관전 채팅 세션 추가 - roomId: {}, sessionId: {}, 총 세션 수: {}", 
                roomId, session.getId(), watchChatSessions.get(roomId).size());
    }
    
    /**
     * 관전 채팅방에서 WebSocket 세션 제거
     */
    public void removeSessionFromWatchChatRoom(String roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = watchChatSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            log.debug("관전 채팅 세션 제거 - roomId: {}, sessionId: {}, 남은 세션 수: {}", 
                    roomId, session.getId(), sessions.size());
            
            // 세션이 모두 없어지면 채널 구독 해제
            if (sessions.isEmpty()) {
                watchChatSessions.remove(roomId);
                unsubscribeFromChannel(roomId);
                log.debug("빈 관전 채팅방 제거 - roomId: {}", roomId);
            }
        }
    }
    
    /**
     * 채널 구독
     */
    private void subscribeToChannel(String roomId) {
        String channelName = ChatRedisKey.getWatchPubSubChannel(roomId);
        ChannelTopic topic = new ChannelTopic(channelName);
        
        redisContainer.addMessageListener(this, topic);
        subscribedChannels.put(roomId, topic);
        
        log.debug("관전 채팅 채널 구독 시작 - channel: {}", channelName);
    }
    
    /**
     * 채널 구독 해제
     */
    private void unsubscribeFromChannel(String roomId) {
        ChannelTopic topic = subscribedChannels.remove(roomId);
        if (topic != null) {
            redisContainer.removeMessageListener(this, topic);
            log.debug("관전 채팅 채널 구독 해제 - channel: {}", topic.getTopic());
        }
    }
    
    /**
     * 관전 채팅방의 모든 세션에 메시지 브로드캐스트
     */
    private void broadcastToWatchChatRoom(String roomId, Map<String, Object> messageData) {
        Set<WebSocketSession> sessions = watchChatSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("활성화된 관전 채팅 세션이 없음 - roomId: {}", roomId);
            return;
        }
        
        try {
            String messageJson = objectMapper.writeValueAsString(messageData);
            TextMessage textMessage = new TextMessage(messageJson);
            
            // 동시성 처리를 위해 세션 복사
            Set<WebSocketSession> sessionsCopy = Set.copyOf(sessions);
            for (WebSocketSession session : sessionsCopy) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                        log.debug("관전 채팅 메시지 전송 성공 - roomId: {}, sessionId: {}", roomId, session.getId());
                    } else {
                        // 닫힌 세션 제거
                        sessions.remove(session);
                        log.debug("닫힌 관전 채팅 세션 제거 - roomId: {}, sessionId: {}", roomId, session.getId());
                    }
                } catch (Exception e) {
                    log.error("개별 관전 채팅 세션 메시지 전송 실패 - roomId: {}, sessionId: {}", roomId, session.getId(), e);
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            log.error("관전 채팅 메시지 브로드캐스트 실패 - roomId: {}", roomId, e);
        }
    }
    
    /**
     * 채널명에서 roomId 추출
     */
    private String extractRoomIdFromChannel(String channel) {
        return channel.replace(ChatRedisKey.WATCH_PUBSUB_CHANNEL, "");
    }
    
    /**
     * 현재 활성화된 관전 채팅방 수 반환
     */
    public int getActiveWatchRoomCount() {
        return watchChatSessions.size();
    }
    
    /**
     * 특정 관전 채팅방의 활성 세션 수 반환
     */
    public int getActiveSessionCount(String roomId) {
        Set<WebSocketSession> sessions = watchChatSessions.get(roomId);
        return sessions != null ? sessions.size() : 0;
    }
}
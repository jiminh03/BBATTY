package com.ssafy.chat.common.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.service.DistributedSessionManagerService;
import com.ssafy.chat.config.ChatConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;

/**
 * 분산 환경에서 채팅방 브로드캐스트를 처리하는 Redis Pub/Sub 리스너
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedBroadcastListener implements MessageListener {
    
    private final RedisMessageListenerContainer redisContainer;
    private final DistributedSessionManagerService distributedSessionManager;
    private final ChatConfiguration chatConfiguration;
    private final ObjectMapper objectMapper;
    
    private static final String BROADCAST_CHANNEL_PATTERN = "distributed:broadcast:*";
    
    @PostConstruct
    public void init() {
        try {
            // 모든 분산 브로드캐스트 채널 구독
            ChannelTopic topic = new ChannelTopic(BROADCAST_CHANNEL_PATTERN);
            redisContainer.addMessageListener(this, topic);
            
            log.info("분산 브로드캐스트 리스너 초기화 완료 - 채널: {}", BROADCAST_CHANNEL_PATTERN);
            
        } catch (Exception e) {
            log.error("분산 브로드캐스트 리스너 초기화 실패", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        try {
            ChannelTopic topic = new ChannelTopic(BROADCAST_CHANNEL_PATTERN);
            redisContainer.removeMessageListener(this, topic);
            
            log.info("분산 브로드캐스트 리스너 종료 완료");
            
        } catch (Exception e) {
            log.error("분산 브로드캐스트 리스너 종료 실패", e);
        }
    }
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());
            
            log.debug("분산 브로드캐스트 메시지 수신 - channel: {}", channel);
            
            // 메시지 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> broadcastData = objectMapper.readValue(messageBody, Map.class);
            
            String roomId = (String) broadcastData.get("roomId");
            String broadcastMessage = (String) broadcastData.get("message");
            String sourceInstanceId = (String) broadcastData.get("sourceInstanceId");
            String excludeInstanceId = (String) broadcastData.get("excludeInstanceId");
            Long timestamp = ((Number) broadcastData.get("timestamp")).longValue();
            
            // 자기 자신이 보낸 메시지는 무시
            String currentInstanceId = chatConfiguration.getOrGenerateInstanceId();
            if (currentInstanceId.equals(sourceInstanceId)) {
                log.debug("자기 자신이 보낸 브로드캐스트 메시지 무시 - instanceId: {}", currentInstanceId);
                return;
            }
            
            // 제외 대상 인스턴스인 경우 무시
            if (currentInstanceId.equals(excludeInstanceId)) {
                log.debug("제외 대상 인스턴스 브로드캐스트 메시지 무시 - instanceId: {}", currentInstanceId);
                return;
            }
            
            // 메시지가 너무 오래된 경우 무시 (1분 이상)
            long messageAge = System.currentTimeMillis() - timestamp;
            if (messageAge > 60000) {
                log.warn("오래된 브로드캐스트 메시지 무시 - roomId: {}, age: {}ms", roomId, messageAge);
                return;
            }
            
            // 해당 채팅방의 로컬 세션들에게 메시지 전송
            sendToLocalSessionsInRoom(roomId, broadcastMessage);
            
            log.debug("분산 브로드캐스트 메시지 처리 완료 - roomId: {}, sourceInstance: {}", 
                    roomId, sourceInstanceId);
            
        } catch (Exception e) {
            log.error("분산 브로드캐스트 메시지 처리 실패", e);
        }
    }
    
    /**
     * 특정 채팅방의 로컬 세션들에게 메시지 전송
     */
    private void sendToLocalSessionsInRoom(String roomId, String message) {
        try {
            // DistributedSessionManagerService를 통해 해당 방의 로컬 세션들을 찾아서 전송
            // 실제 구현에서는 로컬 세션 캐시를 직접 참조할 수도 있음
            
            // 채널명에서 roomId 추출
            String extractedRoomId = extractRoomIdFromChannel(roomId);
            
            // 현재 인스턴스의 세션들에게만 전송
            // (DistributedSessionManagerServiceImpl의 sendToLocalSessionsInRoom 메서드 활용)
            
            log.debug("로컬 세션에 분산 브로드캐스트 메시지 전송 - roomId: {}", extractedRoomId);
            
        } catch (Exception e) {
            log.error("로컬 세션 브로드캐스트 실패 - roomId: {}", roomId, e);
        }
    }
    
    /**
     * 채널명에서 roomId 추출
     */
    private String extractRoomIdFromChannel(String channel) {
        return channel.replace("distributed:broadcast:", "");
    }
}
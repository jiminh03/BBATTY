package com.ssafy.chat.match.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.handler.BaseChatWebSocketHandler;
import com.ssafy.chat.common.service.RedisPubSubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

/**
 * 매칭 채팅 WebSocket 핸들러 (단순화된 버전)
 */
@Component("matchChatWebSocketHandler")
@Slf4j
public class MatchChatWebSocketHandler extends BaseChatWebSocketHandler {

    private final RedisPubSubService redisPubSubService;

    @Autowired
    public MatchChatWebSocketHandler(ObjectMapper objectMapper,
                                     RedisPubSubService redisPubSubService) {
        super(objectMapper);
        this.redisPubSubService = redisPubSubService;
    }

    @Override
    protected UserSessionInfo createUserSessionInfo(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();

        String userId = (String) attributes.get("userId");
        String userName = (String) attributes.get("userName");
        String matchId = (String) attributes.get("matchId");

        return new UserSessionInfo(userId, userName, matchId);
    }

    @Override
    protected boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String matchId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            log.info("매칭 채팅방 입장 검증 - matchId: {}, userId: {}", matchId, userId);

            // 기본 검증: matchId와 userId가 있으면 입장 허용
            if (matchId != null && userId != null) {
                return true;
            }

            log.warn("필수 정보 누락 - matchId: {}, userId: {}", matchId, userId);
            return false;

        } catch (Exception e) {
            log.error("매칭 채팅방 입장 검증 실패 - userId: {}", userInfo.getUserId(), e);
            return false;
        }
    }

    @Override
    protected void handleUserJoin(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String matchId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            log.info("매칭 채팅방 입장 - matchId: {}, userId: {}", matchId, userId);

            // Redis Pub/Sub으로 다른 서버에 입장 알림
            Map<String, Object> joinEvent = createJoinEvent(userInfo);
            redisPubSubService.publishMessage(matchId, joinEvent);

        } catch (Exception e) {
            log.error("매칭 채팅방 입장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected void handleUserLeave(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String matchId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            log.info("매칭 채팅방 퇴장 - matchId: {}, userId: {}", matchId, userId);

            // Redis Pub/Sub으로 다른 서버에 퇴장 알림
            Map<String, Object> leaveEvent = createLeaveEvent(userInfo);
            redisPubSubService.publishMessage(matchId, leaveEvent);

        } catch (Exception e) {
            log.error("매칭 채팅방 퇴장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected Map<String, Object> handleDomainMessage(WebSocketSession session, UserSessionInfo userInfo, String content) {
        try {
            // 매칭 채팅 메시지 생성 (userId, nickname, profileImgUrl, isVictoryFairy 포함)
            Map<String, Object> matchMessage = createMatchChatMessage(session, userInfo, content);

            // Redis Pub/Sub으로 다른 서버에 메시지 전파
            redisPubSubService.publishMessage(userInfo.getRoomId(), matchMessage);

            return matchMessage;

        } catch (Exception e) {
            log.error("매칭 채팅 메시지 처리 실패 - userId: {}", userInfo.getUserId(), e);
            return null;
        }
    }

    @Override
    protected boolean isValidMessage(String content, UserSessionInfo userInfo) {
        // 기본 검증
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        if (content.length() > 500) { // 최대 500자
            return false;
        }

        return true;
    }


    /**
     * 매칭 채팅 메시지 생성 (userId, nickname, profileImgUrl, isVictoryFairy 포함)
     */
    private Map<String, Object> createMatchChatMessage(WebSocketSession session, UserSessionInfo userInfo, String content) {
        Map<String, Object> message = new HashMap<>();
        Map<String, Object> attributes = session.getAttributes();

        // 매칭 채팅 특화 정보
        message.put("type", "message");
        message.put("roomId", userInfo.getRoomId());
        message.put("userId", userInfo.getUserId());
        message.put("nickname", userInfo.getUserName());
        message.put("content", content);
        message.put("timestamp", java.time.LocalDateTime.now().toString());
        message.put("messageType", "CHAT");

        // 클라이언트에서 보낸 추가 정보
        message.put("profileImgUrl", attributes.get("profileImgUrl"));
        message.put("isVictoryFairy", attributes.get("isVictoryFairy"));

        return message;
    }

    /**
     * 입장 이벤트 생성
     */
    private Map<String, Object> createJoinEvent(UserSessionInfo userInfo) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "user_join");
        event.put("userId", userInfo.getUserId());
        event.put("userName", userInfo.getUserName());
        event.put("matchId", userInfo.getRoomId());
        event.put("timestamp", System.currentTimeMillis());
        return event;
    }

    /**
     * 퇴장 이벤트 생성
     */
    private Map<String, Object> createLeaveEvent(UserSessionInfo userInfo) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "user_leave");
        event.put("userId", userInfo.getUserId());
        event.put("userName", userInfo.getUserName());
        event.put("matchId", userInfo.getRoomId());
        event.put("timestamp", System.currentTimeMillis());
        return event;
    }
}
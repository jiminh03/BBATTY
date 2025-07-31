package com.ssafy.chat.watch.handler;

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
 * 게임 채팅 WebSocket 핸들러 (단순화된 버전)
 */
@Component("gameChatWebSocketHandler")
@Slf4j
public class GameChatWebSocketHandler extends BaseChatWebSocketHandler {

    private final RedisPubSubService redisPubSubService;

    @Autowired
    public GameChatWebSocketHandler(ObjectMapper objectMapper,
                                    RedisPubSubService redisPubSubService) {
        super(objectMapper);
        this.redisPubSubService = redisPubSubService;
    }

    @Override
    protected UserSessionInfo createUserSessionInfo(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();

        String userId = (String) attributes.get("userId");
        String userName = (String) attributes.get("userName");
        String teamId = (String) attributes.get("teamId");

        return new UserSessionInfo(userId, userName, teamId);
    }

    @Override
    protected boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId(); // 채팅방 팀 ID
            String userTeamId = (String) session.getAttributes().get("userTeamId"); // 사용자 응원팀
            
            log.info("채팅방 입장 검증 - 채팅방팀: {}, 사용자팀: {}", teamId, userTeamId);
            
            // 같은 팀인지 확인
            if (teamId != null && teamId.equals(userTeamId)) {
                log.info("팀 일치 - 입장 허용");
                return true;
            }
            
            log.warn("팀 불일치 - 채팅방팀: {}, 사용자팀: {}", teamId, userTeamId);
            return false;
            
        } catch (Exception e) {
            log.error("채팅방 입장 검증 실패 - userId: {}", userInfo.getUserId(), e);
            return false;
        }
    }

    @Override
    protected void handleUserJoin(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            log.info("게임 채팅방 입장 완료 - teamId: {}, userId: {}", teamId, userId);

        } catch (Exception e) {
            log.error("게임 채팅방 입장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected void handleUserLeave(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId();
            String userId = userInfo.getUserId();

            log.info("게임 채팅방 퇴장 완료 - teamId: {}, userId: {}", teamId, userId);

        } catch (Exception e) {
            log.error("게임 채팅방 퇴장 처리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    @Override
    protected Map<String, Object> handleDomainMessage(WebSocketSession session, UserSessionInfo userInfo, String content) {
        try {
            String teamId = userInfo.getRoomId();

            // 게임 채팅 메시지 생성 (익명 채팅)
            Map<String, Object> gameMessage = createGameChatMessage(userInfo, content);

            // Redis Pub/Sub으로 다른 서버에 메시지 전파
            redisPubSubService.publishMessage(teamId, gameMessage);

            return gameMessage;

        } catch (Exception e) {
            log.error("게임 채팅 메시지 처리 실패 - userId: {}", userInfo.getUserId(), e);
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
     * 게임 채팅 메시지 생성 (익명 채팅)
     */
    private Map<String, Object> createGameChatMessage(UserSessionInfo userInfo, String content) {
        Map<String, Object> message = new HashMap<>();
        
        // 익명 채팅 - nickname 없음
        message.put("type", "message");
        message.put("roomId", userInfo.getRoomId());
        message.put("content", content);
        message.put("timestamp", java.time.LocalDateTime.now().toString());
        message.put("messageType", "CHAT");
        
        return message;
    }
}
package com.ssafy.chat.watch.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.UserSessionInfo;
import com.ssafy.chat.common.enums.MessageType;
import com.ssafy.chat.watch.dto.WatchChatMessage;
import com.ssafy.chat.watch.service.WatchChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 관전 채팅 WebSocket 핸들러 (완전 독립)
 * WatchChatService를 통해 Redis Pub/Sub과 연동
 */
@Component("watchChatWebSocketHandler")
@Slf4j
@RequiredArgsConstructor
public class WatchChatWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WatchChatService watchChatService;
    
    // 로컬 세션 관리 (Redis Subscriber와 별도)
    private final Map<String, Set<WebSocketSession>> connectedUsers = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, UserSessionInfo> sessionToUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            UserSessionInfo userInfo = createUserSessionInfo(session);

            if (!canJoinChatRoom(session, userInfo)) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Access denied"));
                return;
            }

            // 관전 채팅: 단일 세션만 허용 (기존 연결 해제)
            handleConnectionManagement(session, userInfo);

            // 로컬 세션 정보 저장
            connectedUsers.computeIfAbsent(userInfo.getUserId(), k -> ConcurrentHashMap.newKeySet()).add(session);
            sessionToUser.put(session, userInfo);

            // Redis Subscriber에 세션 등록
            watchChatService.addSessionToWatchRoom(userInfo.getRoomId(), session);

            log.info("관전 채팅 WebSocket 연결 성공 - userId: {}, teamId: {}, sessionId: {}",
                    userInfo.getUserId(), userInfo.getRoomId(), session.getId());

        } catch (Exception e) {
            log.error("관전 채팅 WebSocket 연결 처리 실패 - sessionId: {}", session.getId(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            UserSessionInfo userInfo = sessionToUser.get(session);
            if (userInfo != null) {
                String teamId = userInfo.getRoomId();
                
                // Redis Subscriber에서 세션 제거
                watchChatService.removeSessionFromWatchRoom(teamId, session);
                
                // 로컬 세션 정보 제거
                Set<WebSocketSession> userSessions = connectedUsers.get(userInfo.getUserId());
                if (userSessions != null) {
                    userSessions.remove(session);
                    if (userSessions.isEmpty()) {
                        connectedUsers.remove(userInfo.getUserId());
                    }
                }
                sessionToUser.remove(session);

                log.info("관전 채팅 WebSocket 연결 종료 - userId: {}, teamId: {}, status: {}",
                        userInfo.getUserId(), teamId, status);
            }
        } catch (Exception e) {
            log.error("관전 채팅 WebSocket 연결 종료 처리 실패 - sessionId: {}", session.getId(), e);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            UserSessionInfo userInfo = sessionToUser.get(session);
            if (userInfo == null) {
                log.warn("세션 정보 없음 - sessionId: {}", session.getId());
                return;
            }

            if (message instanceof TextMessage) {
                String messageContent = ((TextMessage) message).getPayload();

                // 메시지 검증
                if (!isValidMessage(messageContent, userInfo)) {
                    log.warn("유효하지 않은 메시지 - userId: {}", userInfo.getUserId());
                    return;
                }

                String teamId = userInfo.getRoomId();

                // 트래픽 카운트 증가 (급증 모니터링용)
                watchChatService.incrementTrafficCount(teamId);
                
                // 트래픽 급증 감지 (백그라운드에서 비동기 처리)
                watchChatService.checkTrafficSpike(teamId);

                // 관전 채팅 메시지 생성 (익명 채팅)
                WatchChatMessage chatMessage = createWatchChatMessage(userInfo, messageContent);

                // Redis Pub/Sub으로 메시지 발송 (브로드캐스트는 Subscriber에서 처리)
                watchChatService.sendChatMessage(teamId, chatMessage);
            }

        } catch (Exception e) {
            log.error("관전 채팅 메시지 처리 실패 - sessionId: {}", session.getId(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("관전 채팅 WebSocket 전송 오류 - sessionId: {}", session.getId(), exception);

        UserSessionInfo userInfo = sessionToUser.get(session);
        if (userInfo != null) {
            watchChatService.removeSessionFromWatchRoom(userInfo.getRoomId(), session);
        }

        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 사용자 세션 정보 생성
     */
    private UserSessionInfo createUserSessionInfo(WebSocketSession session) {
        try {
            // Query parameter에서 세션 토큰 추출
            URI uri = session.getUri();
            if (uri == null) {
                throw new IllegalArgumentException("WebSocket URI is null");
            }

            String query = uri.getQuery();
            Map<String, String> params = UriComponentsBuilder.fromUriString("?" + query)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();

            String sessionToken = params.get("token");
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                throw new IllegalArgumentException("Session token is required");
            }

            // 세션 토큰 검증
            if (!watchChatService.validateUserSession(sessionToken)) {
                throw new IllegalArgumentException("Invalid or expired session token");
            }

            // 세션에서 사용자 정보 조회
            Map<String, Object> sessionData = watchChatService.getUserInfoFromSession(sessionToken);
            if (sessionData.isEmpty()) {
                throw new IllegalArgumentException("Session data not found");
            }

            String userId = (String) sessionData.get("userId");
            String userName = (String) sessionData.get("userName");
            String userTeamId = (String) sessionData.get("userTeamId");
            String teamId = params.get("teamId"); // 채팅방 팀 ID는 query parameter에서

            if (userId == null || userName == null || userTeamId == null || teamId == null) {
                throw new IllegalArgumentException("Missing required session data");
            }

            // 세션에 필요한 정보 저장
            Map<String, Object> attributes = session.getAttributes();
            attributes.put("userId", userId);
            attributes.put("userName", userName);
            attributes.put("userTeamId", userTeamId);
            attributes.put("teamId", teamId);
            attributes.put("sessionToken", sessionToken);

            return new UserSessionInfo(userId, userName, teamId);

        } catch (Exception e) {
            log.error("관전 채팅 세션 정보 생성 실패", e);
            throw new IllegalArgumentException("Failed to create user session info: " + e.getMessage());
        }
    }

    /**
     * 채팅방 입장 가능 여부 확인
     */
    private boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String teamId = userInfo.getRoomId(); // 채팅방 팀 ID
            String userTeamId = (String) session.getAttributes().get("userTeamId"); // 사용자 응원팀
            
            log.info("관전 채팅방 입장 검증 - 채팅방팀: {}, 사용자팀: {}", teamId, userTeamId);
            
            // 같은 팀인지 확인
            if (teamId != null && teamId.equals(userTeamId)) {
                log.info("팀 일치 - 입장 허용");
                return true;
            }
            
            log.warn("팀 불일치 - 채팅방팀: {}, 사용자팀: {}", teamId, userTeamId);
            return false;
            
        } catch (Exception e) {
            log.error("관전 채팅방 입장 검증 실패 - userId: {}", userInfo.getUserId(), e);
            return false;
        }
    }

    /**
     * 연결 관리 처리 (관전 채팅: 단일 세션만 허용)
     */
    private void handleConnectionManagement(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String userId = userInfo.getUserId();
            
            // 기존 연결이 있는지 확인
            Set<WebSocketSession> existingSessions = connectedUsers.get(userId);
            if (existingSessions != null && !existingSessions.isEmpty()) {
                log.info("관전 채팅 - 기존 연결 해제 - userId: {}, 기존 세션 수: {}", 
                        userId, existingSessions.size());
                
                // 기존 모든 세션 종료
                for (WebSocketSession existingSession : existingSessions) {
                    if (existingSession.isOpen()) {
                        try {
                            existingSession.close(CloseStatus.NORMAL.withReason("New connection established"));
                        } catch (Exception e) {
                            log.warn("기존 세션 종료 실패 - sessionId: {}", existingSession.getId(), e);
                        }
                    }
                }
                
                // 맵에서 제거
                existingSessions.clear();
                connectedUsers.remove(userId);
                
                // sessionToUser 맵에서도 제거
                sessionToUser.entrySet().removeIf(entry -> 
                    entry.getValue().getUserId().equals(userId));
            }
            
            log.info("관전 채팅 - 단일 세션 허용 - userId: {}", userId);
            
        } catch (Exception e) {
            log.error("관전 채팅 연결 관리 실패 - userId: {}", userInfo.getUserId(), e);
        }
    }

    /**
     * 메시지 유효성 검증
     */
    private boolean isValidMessage(String content, UserSessionInfo userInfo) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        if (content.length() > 500) { // 최대 500자
            return false;
        }

        return true;
    }

    /**
     * 관전 채팅 메시지 생성 (익명 채팅)
     */
    private WatchChatMessage createWatchChatMessage(UserSessionInfo userInfo, String content) {
        WatchChatMessage message = new WatchChatMessage();
        
        message.setMessageType(MessageType.CHAT);
        message.setRoomId(userInfo.getRoomId());
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        
        // 관전 채팅 특화 정보
        message.setTrafficTimestamp(System.currentTimeMillis());
        
        return message;
    }
}
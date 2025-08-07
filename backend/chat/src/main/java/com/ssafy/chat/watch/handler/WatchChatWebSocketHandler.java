package com.ssafy.chat.watch.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.UserSessionInfo;
import com.ssafy.chat.common.enums.ChatRoomType;
import com.ssafy.chat.common.enums.MessageType;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
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
            // HandshakeInterceptor에서 설정한 세션 속성 사용
            Map<String, Object> attributes = session.getAttributes();
            
            // 채팅 타입 확인
            String chatType = (String) attributes.get("chatType");
            if (!"watch".equals(chatType)) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "잘못된 채팅 타입입니다: " + chatType);
            }

            // HandshakeInterceptor에서 설정한 속성들 추출
            Long teamId = (Long) attributes.get("teamId");
            String gameId = (String) attributes.get("gameId");
            Boolean isAttendanceVerified = (Boolean) attributes.get("isAttendanceVerified");

            if (teamId == null || gameId == null) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "필수 세션 정보가 누락되었습니다.");
            }

            // 직관 채팅은 완전 무명이므로 더미 사용자 정보 생성
            String userId = "anonymous_" + session.getId(); // 세션별 고유 ID
            String userName = "익명_" + teamId; // 팀 기반 익명 이름

            log.info("직관 채팅 사용자 세션 생성 - userId: {}, teamId: {}, gameId: {}", 
                    userId, teamId, gameId);

            return new UserSessionInfo(userId, userName, teamId.toString());

        } catch (Exception e) {
            log.error("관전 채팅 세션 정보 생성 실패", e);
            throw new ApiException(ErrorCode.SERVER_ERROR, "사용자 세션 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 채팅방 입장 가능 여부 확인
     */
    private boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            // HandshakeInterceptor에서 이미 인증 완료했으므로 항상 허용
            // 직관 채팅은 팀별로 분리되어 있고, 이미 올바른 채팅방에 입장한 상태
            Long userTeamId = (Long) session.getAttributes().get("teamId");
            String gameId = (String) session.getAttributes().get("gameId");
            
            log.info("직관 채팅방 입장 허용 - 사용자팀: {}, 게임: {}", userTeamId, gameId);
            return true;
            
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
        
        return message;
    }
}
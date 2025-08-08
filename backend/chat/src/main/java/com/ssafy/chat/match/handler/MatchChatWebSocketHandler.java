package com.ssafy.chat.match.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.UserSessionInfo;
import com.ssafy.chat.common.enums.ChatRoomType;
import com.ssafy.chat.common.enums.MessageType;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.dto.MatchChatMessage;
import com.ssafy.chat.match.service.MatchChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 매칭 채팅 WebSocket 핸들러 (완전 독립)
 * MatchChatService를 통해 Kafka와 연동
 */
@Component("matchChatWebSocketHandler")
@Slf4j
@RequiredArgsConstructor
public class MatchChatWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final MatchChatService matchChatService;
    
    // 로컬 세션 관리 (Kafka Consumer와 별도)
    private final Map<Long, Set<WebSocketSession>> connectedUsers = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, UserSessionInfo> sessionToUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            UserSessionInfo userInfo = createUserSessionInfo(session);

            if (!canJoinChatRoom(session, userInfo)) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Access denied"));
                return;
            }

            // 매칭 채팅: 여러 세션 허용
            handleConnectionManagement(session, userInfo);

            // 로컬 세션 정보 저장
            connectedUsers.computeIfAbsent(userInfo.getUserId(), k -> ConcurrentHashMap.newKeySet()).add(session);
            sessionToUser.put(session, userInfo);

            // Kafka Consumer에 세션 등록
            matchChatService.addSessionToMatchRoom(userInfo.getRoomId(), session);

            // 입장 이벤트 발송
            matchChatService.sendUserJoinEvent(userInfo.getRoomId(), userInfo.getUserId(), userInfo.getNickname());

            log.info("매칭 채팅 WebSocket 연결 성공 - userId: {}, matchId: {}, sessionId: {}",
                    userInfo.getUserId(), userInfo.getRoomId(), session.getId());

        } catch (Exception e) {
            log.error("매칭 채팅 WebSocket 연결 처리 실패 - sessionId: {}", session.getId(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            UserSessionInfo userInfo = sessionToUser.get(session);
            if (userInfo != null) {
                String matchId = userInfo.getRoomId();
                
                // Kafka Consumer에서 세션 제거
                matchChatService.removeSessionFromMatchRoom(matchId, session);
                
                // 퇴장 이벤트 발송
                log.info("매칭 채팅 연결 종료 - leave 이벤트 발송 - userId: {}, status: {}", userInfo.getUserId(), status);
                matchChatService.sendUserLeaveEvent(matchId, userInfo.getUserId(), userInfo.getNickname());
                
                // 로컬 세션 정보 제거
                Set<WebSocketSession> userSessions = connectedUsers.get(userInfo.getUserId());
                if (userSessions != null) {
                    userSessions.remove(session);
                    if (userSessions.isEmpty()) {
                        connectedUsers.remove(userInfo.getUserId());
                    }
                }
                sessionToUser.remove(session);

                log.info("매칭 채팅 WebSocket 연결 종료 - userId: {}, matchId: {}, status: {}",
                        userInfo.getUserId(), matchId, status);
            }
        } catch (Exception e) {
            log.error("매칭 채팅 WebSocket 연결 종료 처리 실패 - sessionId: {}", session.getId(), e);
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

                // 매칭 채팅 메시지 생성
                MatchChatMessage chatMessage = createMatchChatMessage(session, userInfo, messageContent);

                // Kafka로 메시지 발송 (브로드캐스트는 Consumer에서 처리)
                matchChatService.sendChatMessage(userInfo.getRoomId(), chatMessage);
            }

        } catch (Exception e) {
            log.error("매칭 채팅 메시지 처리 실패 - sessionId: {}", session.getId(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("매칭 채팅 WebSocket 전송 오류 - sessionId: {}", session.getId(), exception);

        UserSessionInfo userInfo = sessionToUser.get(session);
        if (userInfo != null) {
            matchChatService.removeSessionFromMatchRoom(userInfo.getRoomId(), session);
            matchChatService.sendUserLeaveEvent(userInfo.getRoomId(), userInfo.getUserId(), userInfo.getNickname());
        }

        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 사용자 세션 정보 생성 (수정된 버전)
     */
    private UserSessionInfo createUserSessionInfo(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();

        Object userIdObj = attributes.get("userId");
        Object nicknameObj = attributes.get("nickname");  // ✅ userName → nickname으로 변경
        Object matchIdObj = attributes.get("matchId");

        // null 체크 후 안전한 변환
        if (userIdObj == null || nicknameObj == null || matchIdObj == null) {
            // ✅ 디버깅을 위한 상세 로그
            log.error("필수 세션 정보 누락 상세:");
            log.error("- userId: {} (존재: {})", userIdObj, userIdObj != null);
            log.error("- nickname: {} (존재: {})", nicknameObj, nicknameObj != null);
            log.error("- matchId: {} (존재: {})", matchIdObj, matchIdObj != null);

            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        Long userId = ((Number) userIdObj).longValue();
        String nickname = String.valueOf(nicknameObj);  //nickname 변수명으로 변경
        String matchId = String.valueOf(matchIdObj);

        return new UserSessionInfo(userId, nickname, matchId);  // nickname 전달
    }

    /**
     * 채팅방 입장 가능 여부 확인
     */
    private boolean canJoinChatRoom(WebSocketSession session, UserSessionInfo userInfo) {
        try {
            String matchId = userInfo.getRoomId();
            Long userId = userInfo.getUserId();

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

    /**
     * 연결 관리 처리 (매칭 채팅: 여러 세션 허용)
     */
    private void handleConnectionManagement(WebSocketSession session, UserSessionInfo userInfo) {
        log.info("매칭 채팅 - 다중 세션 허용 - userId: {}", userInfo.getUserId());
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
     * 매칭 채팅 메시지 생성
     */
    private MatchChatMessage createMatchChatMessage(WebSocketSession session, UserSessionInfo userInfo, String content) {
        Map<String, Object> attributes = session.getAttributes();

        MatchChatMessage message = new MatchChatMessage();
        message.setMessageType(MessageType.CHAT);
        message.setRoomId(userInfo.getRoomId());
        message.setContent(content);
        message.setTimestamp(java.time.LocalDateTime.now());
        
        // 매칭 채팅 특화 정보
        message.setUserId(userInfo.getUserId());
        message.setNickname(userInfo.getNickname());
        message.setProfileImgUrl((String) attributes.get("profileImgUrl"));
        message.setWinFairy((Boolean) attributes.getOrDefault("isWinFairy", false));

        return message;
    }
}
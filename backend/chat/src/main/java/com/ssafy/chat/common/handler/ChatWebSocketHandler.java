package com.ssafy.chat.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.SessionInfo;
import com.ssafy.chat.common.dto.SessionTokenInfo;
import com.ssafy.chat.common.service.DistributedSessionManagerService;
import com.ssafy.chat.common.service.SessionTokenService;
import com.ssafy.chat.common.util.KSTTimeUtil;
import com.ssafy.chat.common.util.TestModeUtil;
import com.ssafy.chat.common.enums.MessageType;
import com.ssafy.chat.match.service.MatchChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 통합 채팅 WebSocket 핸들러
 * sessionToken 기반으로 인증된 사용자만 WebSocket 연결 허용
 * REST API에서 사전에 방 존재 여부와 인증을 완료한 상태
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final SessionTokenService sessionTokenService;
    private final DistributedSessionManagerService sessionManager;
    private final ObjectMapper objectMapper;
    private final MatchChatService matchChatService;
    private final TestModeUtil testModeUtil;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            log.debug("WebSocket 연결 시도 - sessionId: {}", session.getId());
            
            // 1. URL에서 sessionToken 추출
            String sessionToken = extractSessionToken(session);
            if (sessionToken == null) {
                sendErrorAndClose(session, "SESSION_TOKEN_REQUIRED", "세션 토큰이 필요합니다.");
                return;
            }
            
            // 🧪 테스트 모드 체크 및 처리
            SessionTokenInfo tokenInfo;
            if (testModeUtil.isTestMode(sessionToken)) {
                log.info("🧪 WebSocket 핸들러 테스트 모드 활성화 - sessionToken: {}", sessionToken);
                tokenInfo = testModeUtil.createTestTokenInfo(sessionToken, session);
                if (tokenInfo == null) {
                    sendErrorAndClose(session, "TEST_TOKEN_ERROR", "테스트 토큰 생성에 실패했습니다.");
                    return;
                }
            } else {
                // 2. sessionToken 검증 (이미 방 존재 여부까지 확인된 토큰)
                tokenInfo = sessionTokenService.validateToken(sessionToken);
                if (tokenInfo == null || !tokenInfo.isValid()) {
                    sendErrorAndClose(session, "INVALID_SESSION_TOKEN", "유효하지 않은 세션 토큰입니다.");
                    return;
                }
            }
            
            // 3. 세션 정보 생성
            SessionInfo sessionInfo = createSessionInfo(tokenInfo, session);
            
            // 4. 분산 세션 매니저에 등록
            sessionManager.registerSession(tokenInfo.getRoomId(), session, sessionInfo);
            
            // 5. 매치 채팅인 경우 히스토리 로드를 위해 매치 채팅 서비스에도 세션 등록
            if (tokenInfo.isMatchRoom()) {
                matchChatService.addSessionToMatchRoom(tokenInfo.getRoomId(), session);
                log.debug("매치 채팅 서비스에 세션 추가 완료 - roomId: {}, sessionId: {}", 
                        tokenInfo.getRoomId(), session.getId());
            }
            
            // 6. 연결 성공 알림
            sendConnectionSuccess(session, tokenInfo);
            
            log.info("WebSocket 연결 성공 - roomId: {}, userId: {}, sessionId: {}", 
                    tokenInfo.getRoomId(), tokenInfo.getUserId(), session.getId());
                    
        } catch (Exception e) {
            log.error("WebSocket 연결 처리 실패 - sessionId: {}", session.getId(), e);
            sendErrorAndClose(session, "CONNECTION_ERROR", "연결 처리 중 오류가 발생했습니다.");
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            // 세션 토큰에서 룸 정보 추출
            String sessionToken = extractSessionToken(session);
            if (sessionToken != null) {
                // 🧪 테스트 모드 체크 및 처리
                SessionTokenInfo tokenInfo;
                if (testModeUtil.isTestMode(sessionToken)) {
                    tokenInfo = testModeUtil.createTestTokenInfo(sessionToken, session);
                } else {
                    tokenInfo = sessionTokenService.validateToken(sessionToken);
                }
                
                if (tokenInfo != null) {
                    // 분산 세션 매니저에서 해제
                    sessionManager.unregisterSession(tokenInfo.getRoomId(), session.getId());
                    
                    // 매치 채팅인 경우 매치 채팅 서비스에서도 세션 해제
                    if (tokenInfo.isMatchRoom()) {
                        matchChatService.removeSessionFromMatchRoom(tokenInfo.getRoomId(), session);
                        log.debug("매치 채팅 서비스에서 세션 제거 완료 - roomId: {}, sessionId: {}", 
                                tokenInfo.getRoomId(), session.getId());
                    }
                    
                    log.info("WebSocket 연결 종료 - roomId: {}, userId: {}, status: {}", 
                            tokenInfo.getRoomId(), tokenInfo.getUserId(), status);
                }
            }
            
        } catch (Exception e) {
            log.error("WebSocket 연결 종료 처리 실패 - sessionId: {}", session.getId(), e);
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            // 메시지 처리는 각 채팅 서비스에 위임
            String payload = message.getPayload();
            log.debug("메시지 수신 - sessionId: {}, payload: {}", session.getId(), payload);
            
            // 세션 토큰으로 방 정보 확인
            String sessionToken = extractSessionToken(session);
            if (sessionToken == null) {
                sendError(session, "INVALID_SESSION", "유효하지 않은 세션입니다.");
                return;
            }
            
            // 🧪 테스트 모드 체크 및 처리  
            SessionTokenInfo tokenInfo;
            if (testModeUtil.isTestMode(sessionToken)) {
                tokenInfo = testModeUtil.createTestTokenInfo(sessionToken, session);
                if (tokenInfo == null) {
                    sendError(session, "TEST_TOKEN_ERROR", "테스트 토큰 생성에 실패했습니다.");
                    return;
                }
            } else {
                tokenInfo = sessionTokenService.validateToken(sessionToken);
                if (tokenInfo == null) {
                    sendError(session, "SESSION_EXPIRED", "세션이 만료되었습니다.");
                    return;
                }
            }
            
            // 단순 텍스트 메시지 처리
            handleChatMessage(session, tokenInfo, payload);
            
        } catch (Exception e) {
            log.error("메시지 처리 실패 - sessionId: {}", session.getId(), e);
            sendError(session, "MESSAGE_PROCESS_ERROR", "메시지 처리 중 오류가 발생했습니다.");
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 전송 오류 - sessionId: {}", session.getId(), exception);
        
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (Exception e) {
            log.error("전송 오류 후 세션 종료 실패", e);
        }
    }
    
    // ===========================================
    // 내부 헬퍼 메서드
    // ===========================================
    
    /**
     * URL에서 sessionToken 추출
     */
    private String extractSessionToken(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                String query = uri.getQuery();
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if ("sessionToken".equals(keyValue[0]) && keyValue.length > 1) {
                        return keyValue[1];
                    }
                }
            }
            
            // 헤더에서도 확인
            Object tokenHeader = session.getAttributes().get("sessionToken");
            if (tokenHeader != null) {
                return tokenHeader.toString();
            }
            
        } catch (Exception e) {
            log.error("sessionToken 추출 실패", e);
        }
        return null;
    }
    
    /**
     * SessionInfo 생성
     */
    private SessionInfo createSessionInfo(SessionTokenInfo tokenInfo, WebSocketSession session) {
        long kstNow = KSTTimeUtil.nowAsTimestamp();
        return SessionInfo.builder()
            .sessionToken(tokenInfo.getToken())
            .userId(tokenInfo.getUserId())
            .nickname(tokenInfo.getNickname())
            .teamId(tokenInfo.getTeamId())
            .teamName(tokenInfo.getTeamName())
            .roomId(tokenInfo.getRoomId())
            .roomType(tokenInfo.getRoomType())
            .gameId(tokenInfo.getGameId())
            .connectedAt(kstNow)
            .lastActivityAt(kstNow)
            .isValid(true)
            .build();
    }
    
    /**
     * 채팅 메시지 처리
     */
    private void handleChatMessage(WebSocketSession session, SessionTokenInfo tokenInfo, String content) {
        try {
            if (content == null || content.trim().isEmpty()) {
                sendError(session, "EMPTY_MESSAGE", "메시지 내용이 비어있습니다.");
                return;
            }
            
            // 매치 채팅인 경우 Kafka를 통한 처리
            if (tokenInfo.isMatchRoom()) {
                handleMatchChatMessage(session, tokenInfo, content.trim());
            } else {
                // 관전 채팅은 분산 세션 매니저를 통해 직접 브로드캐스트
                String broadcastMessage = createBroadcastMessage(tokenInfo, content.trim());
                sessionManager.broadcastToRoom(tokenInfo.getRoomId(), broadcastMessage, null);
            }
            
            log.debug("메시지 브로드캐스트 완료 - roomId: {}, userId: {}, content: {}", 
                    tokenInfo.getRoomId(), tokenInfo.getUserId(), content.trim());
                    
        } catch (Exception e) {
            log.error("채팅 메시지 처리 실패", e);
            sendError(session, "BROADCAST_ERROR", "메시지 전송에 실패했습니다.");
        }
    }
    
    /**
     * 매치 채팅 메시지 처리 (Kafka를 통한 브로드캐스팅)
     */
    private void handleMatchChatMessage(WebSocketSession session, SessionTokenInfo tokenInfo, String content) {
        try {
            // MatchChatMessage 생성
            com.ssafy.chat.match.dto.MatchChatMessage matchMessage = new com.ssafy.chat.match.dto.MatchChatMessage();
            matchMessage.setMessageType(MessageType.CHAT);
            matchMessage.setRoomId(tokenInfo.getRoomId());
            matchMessage.setUserId(tokenInfo.getUserId());
            matchMessage.setNickname(tokenInfo.getNickname() != null ? tokenInfo.getNickname() : "익명");
            matchMessage.setContent(content);
            matchMessage.setTimestamp(KSTTimeUtil.nowAsTimestamp());
            
            // 추가 필드 설정
            matchMessage.setWinFairy(tokenInfo.getIsWinFairy() != null ? tokenInfo.getIsWinFairy() : false);
            
            // SessionTokenInfo에서 profileImgUrl 가져오기
            log.info("채팅 메시지 생성 - tokenInfo.profileImgUrl: {}", tokenInfo.getProfileImgUrl());
            matchMessage.setProfileImgUrl(tokenInfo.getProfileImgUrl());
            
            // 매치 채팅 서비스로 메시지 전송 (Kafka를 통한 브로드캐스팅)
            matchChatService.sendChatMessage(tokenInfo.getRoomId(), matchMessage);
            
            log.debug("매치 채팅 메시지 전송 완료 - roomId: {}, userId: {}", 
                    tokenInfo.getRoomId(), tokenInfo.getUserId());
                    
        } catch (Exception e) {
            log.error("매치 채팅 메시지 처리 실패 - roomId: {}, userId: {}", 
                    tokenInfo.getRoomId(), tokenInfo.getUserId(), e);
        }
    }
    
    /**
     * 브로드캐스트 메시지 생성
     */
    private String createBroadcastMessage(SessionTokenInfo tokenInfo, String content) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "CHAT_MESSAGE");
            message.put("roomId", tokenInfo.getRoomId());
            message.put("userId", tokenInfo.getUserId());
            message.put("nickname", tokenInfo.getNickname() != null ? tokenInfo.getNickname() : "익명");
            message.put("teamName", tokenInfo.getTeamName() != null ? tokenInfo.getTeamName() : "");
            message.put("content", content);
            message.put("timestamp", KSTTimeUtil.nowAsString());
            message.put("messageId", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(message);
            
        } catch (Exception e) {
            log.error("브로드캐스트 메시지 생성 실패", e);
            return "{\"type\":\"ERROR\",\"message\":\"메시지 생성 실패\"}";
        }
    }
    
    /**
     * 연결 성공 알림 전송
     */
    private void sendConnectionSuccess(WebSocketSession session, SessionTokenInfo tokenInfo) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CONNECTION_SUCCESS");
            response.put("roomId", tokenInfo.getRoomId());
            response.put("roomType", tokenInfo.getRoomType());
            response.put("userId", tokenInfo.getUserId());
            response.put("nickname", tokenInfo.getNickname() != null ? tokenInfo.getNickname() : "익명");
            response.put("message", "채팅방에 연결되었습니다.");
            response.put("timestamp", KSTTimeUtil.nowAsString());
            
            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
            
        } catch (Exception e) {
            log.error("연결 성공 알림 전송 실패", e);
        }
    }
    
    /**
     * 에러 메시지 전송 및 연결 종료
     */
    private void sendErrorAndClose(WebSocketSession session, String errorCode, String message) {
        try {
            sendError(session, errorCode, message);
            Thread.sleep(100); // 메시지 전송 완료 대기
            session.close(CloseStatus.POLICY_VIOLATION);
            
        } catch (Exception e) {
            log.error("에러 메시지 전송 및 세션 종료 실패", e);
        }
    }
    
    /**
     * 에러 메시지 전송
     */
    private void sendError(WebSocketSession session, String errorCode, String message) {
        try {
            if (session.isOpen()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("type", "ERROR");
                errorResponse.put("errorCode", errorCode);
                errorResponse.put("message", message);
                errorResponse.put("timestamp", KSTTimeUtil.nowAsString());
                
                String json = objectMapper.writeValueAsString(errorResponse);
                session.sendMessage(new TextMessage(json));
            }
            
        } catch (IOException e) {
            log.error("에러 메시지 전송 실패", e);
        }
    }
}
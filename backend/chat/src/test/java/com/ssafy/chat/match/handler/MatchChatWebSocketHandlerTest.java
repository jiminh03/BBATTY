package com.ssafy.chat.match.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.UserSessionInfo;
import com.ssafy.chat.common.service.RedisPubSubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class MatchChatWebSocketHandlerTest {

    @Mock
    private RedisPubSubService redisPubSubService;
    
    @Mock
    private WebSocketSession session1;
    
    @Mock
    private WebSocketSession session2;
    
    private MatchChatWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new MatchChatWebSocketHandler(objectMapper, redisPubSubService);
    }

    @Test
    @DisplayName("매칭 채팅 - 다중 세션 연결 허용 테스트")
    void testMultipleSessionsAllowed() throws Exception {
        // Given
        String userId = "user1";
        String userName = "testUser";
        String matchId = "match123";
        
        Map<String, Object> attributes1 = createSessionAttributes(userId, userName, matchId);
        Map<String, Object> attributes2 = createSessionAttributes(userId, userName, matchId);
        
        when(session1.getAttributes()).thenReturn(attributes1);
        when(session1.getId()).thenReturn("session1");
        when(session1.isOpen()).thenReturn(true);
        
        when(session2.getAttributes()).thenReturn(attributes2);
        when(session2.getId()).thenReturn("session2");
        when(session2.isOpen()).thenReturn(true);

        // When - 첫 번째 세션 연결
        handler.afterConnectionEstablished(session1);
        
        // Then - 첫 번째 세션이 연결됨
        Set<WebSocketSession> userSessions = handler.getConnectedUserSessions(userId);
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
        assertTrue(userSessions.contains(session1));

        // When - 두 번째 세션 연결 (같은 사용자)
        handler.afterConnectionEstablished(session2);
        
        // Then - 두 번째 세션도 추가로 연결됨 (기존 세션 유지)
        userSessions = handler.getConnectedUserSessions(userId);
        assertNotNull(userSessions);
        assertEquals(2, userSessions.size());
        assertTrue(userSessions.contains(session1));
        assertTrue(userSessions.contains(session2));
        
        verify(session1, never()).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("매칭 채팅 - 세션 정보 생성 테스트")
    void testCreateUserSessionInfo() throws Exception {
        // Given
        String userId = "user1";
        String userName = "testUser";
        String matchId = "match123";
        
        Map<String, Object> attributes = createSessionAttributes(userId, userName, matchId);
        when(session1.getAttributes()).thenReturn(attributes);

        // When
        UserSessionInfo userInfo = handler.createUserSessionInfo(session1);

        // Then
        assertNotNull(userInfo);
        assertEquals(userId, userInfo.getUserId());
        assertEquals(userName, userInfo.getUserName());
        assertEquals(matchId, userInfo.getRoomId());
    }

    @Test
    @DisplayName("매칭 채팅 - 입장 권한 검증 테스트")
    void testCanJoinChatRoom() throws Exception {
        // Given
        Map<String, Object> attributes = createSessionAttributes("user1", "testUser", "match123");
        when(session1.getAttributes()).thenReturn(attributes);
        
        UserSessionInfo userInfo = handler.createUserSessionInfo(session1);

        // When & Then - 정상적인 경우
        assertTrue(handler.canJoinChatRoom(session1, userInfo));

        // When & Then - matchId가 null인 경우
        UserSessionInfo invalidUserInfo = new UserSessionInfo("user1", "testUser", null);
        assertFalse(handler.canJoinChatRoom(session1, invalidUserInfo));
    }

    @Test
    @DisplayName("매칭 채팅 - 메시지 처리 테스트")
    void testHandleMessage() throws Exception {
        // Given
        String userId = "user1";
        String userName = "testUser";
        String matchId = "match123";
        String messageContent = "Hello World!";
        
        Map<String, Object> attributes = createSessionAttributes(userId, userName, matchId);
        when(session1.getAttributes()).thenReturn(attributes);
        when(session1.getId()).thenReturn("session1");
        when(session1.isOpen()).thenReturn(true);

        // 세션 연결
        handler.afterConnectionEstablished(session1);

        // When
        TextMessage message = new TextMessage(messageContent);
        handler.handleMessage(session1, message);

        // Then
        verify(redisPubSubService, times(2)).publishMessage(eq(matchId), any(Map.class));
    }

    @Test
    @DisplayName("매칭 채팅 - 연결 종료 테스트")
    void testAfterConnectionClosed() throws Exception {
        // Given
        String userId = "user1";
        String userName = "testUser";
        String matchId = "match123";
        
        Map<String, Object> attributes = createSessionAttributes(userId, userName, matchId);
        when(session1.getAttributes()).thenReturn(attributes);
        when(session1.getId()).thenReturn("session1");
        when(session1.isOpen()).thenReturn(true);

        // 세션 연결
        handler.afterConnectionEstablished(session1);
        
        // 연결 확인
        Set<WebSocketSession> userSessions = handler.getConnectedUserSessions(userId);
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());

        // When
        handler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        // Then
        userSessions = handler.getConnectedUserSessions(userId);
        assertNull(userSessions); // 모든 세션이 제거되면 userId도 제거됨
        assertNull(handler.getUserSessionInfo(session1));
    }

    @Test
    @DisplayName("매칭 채팅 - 메시지 유효성 검증 테스트")
    void testIsValidMessage() throws Exception {
        // Given
        UserSessionInfo userInfo = new UserSessionInfo("user1", "testUser", "match123");

        // When & Then
        assertTrue(handler.isValidMessage("Valid message", userInfo));
        assertFalse(handler.isValidMessage("", userInfo));
        assertFalse(handler.isValidMessage(null, userInfo));
        assertFalse(handler.isValidMessage("a".repeat(501), userInfo)); // 500자 초과
    }

    private Map<String, Object> createSessionAttributes(String userId, String userName, String matchId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("userName", userName);
        attributes.put("matchId", matchId);
        attributes.put("profileImgUrl", "https://example.com/profile.jpg");
        attributes.put("isVictoryFairy", false);
        return attributes;
    }
}
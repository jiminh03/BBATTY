package com.ssafy.chat.watch.handler;

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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameChatWebSocketHandlerTest {

    @Mock
    private RedisPubSubService redisPubSubService;
    
    @Mock
    private WebSocketSession session1;
    
    @Mock
    private WebSocketSession session2;
    
    private GameChatWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new GameChatWebSocketHandler(objectMapper, redisPubSubService);
    }

    @Test
    @DisplayName("게임 채팅 - 단일 세션만 허용 테스트 (기존 연결 해제)")
    void testSingleSessionOnly() throws Exception {
        // Given
        String userId = "user1";
        String userName = "testUser";
        String teamId = "team1";
        
        Map<String, Object> attributes1 = createSessionAttributes(userId, userName, teamId, teamId);
        Map<String, Object> attributes2 = createSessionAttributes(userId, userName, teamId, teamId);
        
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
        
        // Then - 기존 세션이 종료되고 새 세션만 연결됨
        verify(session1).close(any(CloseStatus.class)); // 기존 세션 종료
        
        userSessions = handler.getConnectedUserSessions(userId);
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
        assertTrue(userSessions.contains(session2));
        assertFalse(userSessions.contains(session1));
    }

    @Test
    @DisplayName("게임 채팅 - 세션 정보 생성 테스트")
    void testCreateUserSessionInfo() throws Exception {
        // Given
        String userId = "user1";
        String userName = "testUser";
        String teamId = "team1";
        
        Map<String, Object> attributes = createSessionAttributes(userId, userName, teamId, teamId);
        when(session1.getAttributes()).thenReturn(attributes);

        // When
        UserSessionInfo userInfo = handler.createUserSessionInfo(session1);

        // Then
        assertNotNull(userInfo);
        assertEquals(userId, userInfo.getUserId());
        assertEquals(userName, userInfo.getUserName());
        assertEquals(teamId, userInfo.getRoomId());
    }

    @Test
    @DisplayName("게임 채팅 - 팀 일치 검증 테스트")
    void testCanJoinChatRoom() throws Exception {
        // Given - 팀이 일치하는 경우
        String teamId = "team1";
        String userTeamId = "team1";
        
        Map<String, Object> attributes = createSessionAttributes("user1", "testUser", teamId, userTeamId);
        when(session1.getAttributes()).thenReturn(attributes);
        
        UserSessionInfo userInfo = handler.createUserSessionInfo(session1);

        // When & Then - 팀이 일치하는 경우
        assertTrue(handler.canJoinChatRoom(session1, userInfo));

        // Given - 팀이 일치하지 않는 경우
        attributes.put("userTeamId", "team2");
        when(session1.getAttributes()).thenReturn(attributes);

        // When & Then - 팀이 일치하지 않는 경우
        assertFalse(handler.canJoinChatRoom(session1, userInfo));
    }

    @Test
    @DisplayName("게임 채팅 - 익명 메시지 처리 테스트")
    void testHandleMessage() throws Exception {
        // Given
        String userId = "user1";
        String userName = "testUser";
        String teamId = "team1";
        String messageContent = "Go team!";
        
        Map<String, Object> attributes = createSessionAttributes(userId, userName, teamId, teamId);
        when(session1.getAttributes()).thenReturn(attributes);
        when(session1.getId()).thenReturn("session1");
        when(session1.isOpen()).thenReturn(true);

        // 세션 연결
        handler.afterConnectionEstablished(session1);

        // When
        TextMessage message = new TextMessage(messageContent);
        handler.handleMessage(session1, message);

        // Then
        verify(redisPubSubService).publishMessage(eq(teamId), any(Map.class));
    }

    @Test
    @DisplayName("게임 채팅 - 연결 종료 테스트")
    void testAfterConnectionClosed() throws Exception {
        // Given
        String userId = "user1";
        String userName = "testUser";
        String teamId = "team1";
        
        Map<String, Object> attributes = createSessionAttributes(userId, userName, teamId, teamId);
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
    @DisplayName("게임 채팅 - 메시지 유효성 검증 테스트")
    void testIsValidMessage() throws Exception {
        // Given
        UserSessionInfo userInfo = new UserSessionInfo("user1", "testUser", "team1");

        // When & Then
        assertTrue(handler.isValidMessage("Valid message", userInfo));
        assertFalse(handler.isValidMessage("", userInfo));
        assertFalse(handler.isValidMessage(null, userInfo));
        assertFalse(handler.isValidMessage("a".repeat(501), userInfo)); // 500자 초과
    }

    @Test
    @DisplayName("게임 채팅 - 기존 연결이 닫혀있을 때 정상 처리 테스트")
    void testHandleConnectionManagementWithClosedSession() throws Exception {
        // Given
        String userId = "user1";
        String userName = "testUser";
        String teamId = "team1";
        
        Map<String, Object> attributes1 = createSessionAttributes(userId, userName, teamId, teamId);
        Map<String, Object> attributes2 = createSessionAttributes(userId, userName, teamId, teamId);
        
        when(session1.getAttributes()).thenReturn(attributes1);
        when(session1.getId()).thenReturn("session1");
        when(session1.isOpen()).thenReturn(false); // 이미 닫힌 세션
        
        when(session2.getAttributes()).thenReturn(attributes2);
        when(session2.getId()).thenReturn("session2");
        when(session2.isOpen()).thenReturn(true);

        // 첫 번째 세션 연결 (하지만 이미 닫힌 상태)
        handler.afterConnectionEstablished(session1);

        // When - 두 번째 세션 연결
        handler.afterConnectionEstablished(session2);
        
        // Then - 닫힌 세션에 대해서는 close 호출하지 않음
        verify(session1, never()).close(any(CloseStatus.class));
        
        // 새 세션은 정상 연결됨
        Set<WebSocketSession> userSessions = handler.getConnectedUserSessions(userId);
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
        assertTrue(userSessions.contains(session2));
    }

    private Map<String, Object> createSessionAttributes(String userId, String userName, String teamId, String userTeamId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("userName", userName);
        attributes.put("teamId", teamId);
        attributes.put("userTeamId", userTeamId);
        return attributes;
    }
}
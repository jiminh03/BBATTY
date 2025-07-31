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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GameChatWebSocketHandler 간단 테스트")
class GameChatWebSocketHandlerSimpleTest {

    @Mock
    private RedisPubSubService redisPubSubService;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    
    @Mock
    private WebSocketSession session;
    
    private GameChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GameChatWebSocketHandler(new ObjectMapper(), redisPubSubService, redisTemplate);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("세션 토큰 기반 인증 성공")
    void testSessionTokenAuthentication_Success() throws Exception {
        // Given
        String sessionToken = "valid-session-token";
        String teamId = "team1";
        URI uri = URI.create("ws://localhost:8084/ws/game-chat?token=" + sessionToken + "&teamId=" + teamId);
        
        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("userId", "user123");
        sessionData.put("userName", "testUser");
        sessionData.put("userTeamId", teamId);
        
        when(session.getUri()).thenReturn(uri);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(new HashMap<>());
        
        when(hashOperations.entries("user:session:" + sessionToken)).thenReturn(sessionData);

        // When
        handler.afterConnectionEstablished(session);

        // Then
        Set<WebSocketSession> userSessions = handler.getConnectedUserSessions("user123");
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
        assertTrue(userSessions.contains(session));
    }

    @Test
    @DisplayName("세션 토큰 기반 인증 실패 - 토큰 없음")
    void testSessionTokenAuthentication_NoToken() throws Exception {
        // Given
        URI uri = URI.create("ws://localhost:8084/ws/game-chat?teamId=team1");
        
        when(session.getUri()).thenReturn(uri);
        when(session.getId()).thenReturn("session-1");
        when(session.getAttributes()).thenReturn(new HashMap<>());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            handler.createUserSessionInfo(session);
        });
    }

    @Test
    @DisplayName("세션 토큰 기반 인증 실패 - 유효하지 않은 토큰")
    void testSessionTokenAuthentication_InvalidToken() throws Exception {
        // Given
        String sessionToken = "invalid-session-token";
        String teamId = "team1";
        URI uri = URI.create("ws://localhost:8084/ws/game-chat?token=" + sessionToken + "&teamId=" + teamId);
        
        when(session.getUri()).thenReturn(uri);
        when(session.getId()).thenReturn("session-1");
        when(session.getAttributes()).thenReturn(new HashMap<>());
        
        // Redis에서 빈 결과 반환 (만료되거나 없는 토큰)
        when(hashOperations.entries("user:session:" + sessionToken)).thenReturn(new HashMap<>());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            handler.createUserSessionInfo(session);
        });
    }

    @Test
    @DisplayName("메시지 유효성 검증")
    void testMessageValidation() {
        // Given
        UserSessionInfo userInfo = new UserSessionInfo("user1", "testUser", "team1");

        // When & Then
        assertTrue(handler.isValidMessage("Valid message", userInfo));
        assertFalse(handler.isValidMessage("", userInfo));
        assertFalse(handler.isValidMessage(null, userInfo));
        assertFalse(handler.isValidMessage("a".repeat(501), userInfo)); // 500자 초과
    }

    @Test
    @DisplayName("연결 통계 조회")
    void testConnectionStats() {
        // When
        Map<String, Object> stats = handler.getConnectionStats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalUsers"));
        assertTrue(stats.containsKey("totalSessions"));
        assertTrue(stats.containsKey("timestamp"));
    }
}
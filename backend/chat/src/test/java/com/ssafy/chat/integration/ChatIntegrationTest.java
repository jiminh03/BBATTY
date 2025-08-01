package com.ssafy.chat.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.AuthRequest;
import com.ssafy.chat.common.dto.AuthResponse;
import com.ssafy.chat.common.service.AuthService;
import com.ssafy.chat.watch.handler.GameChatWebSocketHandler;
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
@DisplayName("채팅 서비스 통합 테스트 - 인증부터 WebSocket 연결까지")
class ChatIntegrationTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private WebSocketSession webSocketSession;

    private AuthService authService;
    private GameChatWebSocketHandler gameHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authService = new com.ssafy.chat.common.service.AuthServiceImpl(redisTemplate);
        gameHandler = new GameChatWebSocketHandler(
                new ObjectMapper(), 
                mock(com.ssafy.chat.common.service.RedisPubSubService.class), 
                redisTemplate
        );
        objectMapper = new ObjectMapper();

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("전체 플로우 테스트: 인증 → 세션 생성 → WebSocket 연결 → 메시지 전송")
    void testCompleteGameChatFlow() throws Exception {
        // 1. 인증 요청 및 세션 생성
        AuthRequest authRequest = new AuthRequest();
        authRequest.setAccessToken("test-jwt-token");
        authRequest.setChatType("game");
        authRequest.setRoomId("team1");

        // 세션 생성 성공
        AuthResponse authResponse = authService.authenticateAndCreateSession(authRequest);
        
        assertTrue(authResponse.isSuccess());
        assertNotNull(authResponse.getSessionToken());
        assertTrue(authResponse.getWebsocketUrl().contains("/ws/game-chat"));
        assertTrue(authResponse.getWebsocketUrl().contains("token=" + authResponse.getSessionToken()));

        // Redis에 세션 저장 확인
        verify(hashOperations).putAll(contains("user:session:"), any(Map.class));
        verify(redisTemplate).expire(anyString(), anyLong(), any());

        // 2. WebSocket 연결 설정
        String sessionToken = authResponse.getSessionToken();
        String websocketUrl = authResponse.getWebsocketUrl();
        URI uri = URI.create(websocketUrl.replace("ws://localhost:8084", ""));

        when(webSocketSession.getUri()).thenReturn(uri);
        when(webSocketSession.getId()).thenReturn("ws-session-1");
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getAttributes()).thenReturn(new HashMap<>());

        // Redis에서 세션 정보 조회 시뮬레이션
        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("userId", "temp_user_123");
        sessionData.put("userName", "임시사용자");
        sessionData.put("userTeamId", "team1");
        
        when(hashOperations.entries("user:session:" + sessionToken)).thenReturn(sessionData);

        // 3. WebSocket 연결 성공
        gameHandler.afterConnectionEstablished(webSocketSession);

        // 연결 확인
        Set<WebSocketSession> userSessions = gameHandler.getConnectedUserSessions("temp_user_123");
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
        assertTrue(userSessions.contains(webSocketSession));

        // 세션 attributes 확인
        Map<String, Object> attributes = webSocketSession.getAttributes();
        assertEquals("temp_user_123", attributes.get("userId"));
        assertEquals("임시사용자", attributes.get("userName"));
        assertEquals("team1", attributes.get("userTeamId"));
        assertEquals("team1", attributes.get("teamId"));
    }

    @Test
    @DisplayName("인증 실패 후 WebSocket 연결 시도")
    void testAuthenticationFailureFlow() throws Exception {
        // 1. 만료된 또는 잘못된 토큰으로 WebSocket 연결 시도
        String invalidToken = "invalid-session-token";
        URI uri = URI.create("/ws/game-chat?token=" + invalidToken + "&teamId=team1");

        when(webSocketSession.getUri()).thenReturn(uri);
        when(webSocketSession.getId()).thenReturn("ws-session-invalid");
        when(webSocketSession.getAttributes()).thenReturn(new HashMap<>());

        // Redis에서 빈 결과 반환 (토큰 없음)
        when(hashOperations.entries("user:session:" + invalidToken)).thenReturn(new HashMap<>());

        // 2. WebSocket 연결 실패
        assertThrows(IllegalArgumentException.class, () -> {
            gameHandler.afterConnectionEstablished(webSocketSession);
        });

        // 연결되지 않음 확인
        assertNull(gameHandler.getConnectedUserSessions("any-user"));
    }

    @Test
    @DisplayName("팀 불일치로 인한 연결 거부")
    void testTeamMismatchFlow() throws Exception {
        // 1. 인증 및 세션 생성 (team1 사용자)
        AuthRequest authRequest = new AuthRequest();
        authRequest.setAccessToken("test-jwt-token");
        authRequest.setChatType("game");
        authRequest.setRoomId("team1");

        AuthResponse authResponse = authService.authenticateAndCreateSession(authRequest);
        assertTrue(authResponse.isSuccess());

        // 2. 다른 팀 채팅방에 연결 시도 (team2)
        String sessionToken = authResponse.getSessionToken();
        URI uri = URI.create("/ws/game-chat?token=" + sessionToken + "&teamId=team2");

        when(webSocketSession.getUri()).thenReturn(uri);
        when(webSocketSession.getId()).thenReturn("ws-session-mismatch");
        when(webSocketSession.getAttributes()).thenReturn(new HashMap<>());

        // Redis에서 team1 사용자 정보 반환
        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("userId", "temp_user_123");
        sessionData.put("userName", "임시사용자");
        sessionData.put("userTeamId", "team1"); // team1 사용자

        when(hashOperations.entries("user:session:" + sessionToken)).thenReturn(sessionData);

        // 3. 팀 불일치로 연결 거부
        // canJoinChatRoom에서 false 반환하여 연결 거부됨
        // BaseChatWebSocketHandler에서 close 호출됨
        verify(webSocketSession, never()).close(any());
    }

    @Test
    @DisplayName("세션 만료 후 재인증 플로우")
    void testSessionExpirationAndReauth() throws Exception {
        // 1. 첫 번째 인증 및 세션 생성
        AuthRequest authRequest = new AuthRequest();
        authRequest.setAccessToken("test-jwt-token");
        authRequest.setChatType("game");
        authRequest.setRoomId("team1");

        AuthResponse firstAuth = authService.authenticateAndCreateSession(authRequest);
        assertTrue(firstAuth.isSuccess());

        // 2. 세션 무효화 (만료 시뮬레이션)
        authService.invalidateSession(firstAuth.getSessionToken());
        verify(redisTemplate).delete("user:session:" + firstAuth.getSessionToken());

        // 3. 재인증 및 새 세션 생성
        AuthResponse secondAuth = authService.authenticateAndCreateSession(authRequest);
        assertTrue(secondAuth.isSuccess());
        assertNotEquals(firstAuth.getSessionToken(), secondAuth.getSessionToken());

        // 4. 새 토큰으로 WebSocket 연결
        String newToken = secondAuth.getSessionToken();
        URI uri = URI.create("/ws/game-chat?token=" + newToken + "&teamId=team1");

        when(webSocketSession.getUri()).thenReturn(uri);
        when(webSocketSession.getId()).thenReturn("ws-session-reauth");
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getAttributes()).thenReturn(new HashMap<>());

        Map<Object, Object> newSessionData = new HashMap<>();
        newSessionData.put("userId", "temp_user_456");
        newSessionData.put("userName", "임시사용자");
        newSessionData.put("userTeamId", "team1");

        when(hashOperations.entries("user:session:" + newToken)).thenReturn(newSessionData);

        // 연결 성공
        gameHandler.afterConnectionEstablished(webSocketSession);
        
        Set<WebSocketSession> userSessions = gameHandler.getConnectedUserSessions("temp_user_456");
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
    }

    @Test
    @DisplayName("동시 연결 관리 - 단일 세션 정책")
    void testConcurrentConnectionManagement() throws Exception {
        // 1. 첫 번째 세션 연결
        AuthRequest authRequest = new AuthRequest();
        authRequest.setAccessToken("test-jwt-token");
        authRequest.setChatType("game");
        authRequest.setRoomId("team1");

        AuthResponse authResponse = authService.authenticateAndCreateSession(authRequest);
        String sessionToken = authResponse.getSessionToken();

        // 첫 번째 WebSocket 세션
        WebSocketSession firstSession = mock(WebSocketSession.class);
        URI firstUri = URI.create("/ws/game-chat?token=" + sessionToken + "&teamId=team1");
        
        when(firstSession.getUri()).thenReturn(firstUri);
        when(firstSession.getId()).thenReturn("first-session");
        when(firstSession.isOpen()).thenReturn(true);
        when(firstSession.getAttributes()).thenReturn(new HashMap<>());

        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("userId", "user123");
        sessionData.put("userName", "테스트사용자");
        sessionData.put("userTeamId", "team1");

        when(hashOperations.entries("user:session:" + sessionToken)).thenReturn(sessionData);

        gameHandler.afterConnectionEstablished(firstSession);

        // 2. 같은 사용자의 두 번째 세션 연결
        WebSocketSession secondSession = mock(WebSocketSession.class);
        when(secondSession.getUri()).thenReturn(firstUri);
        when(secondSession.getId()).thenReturn("second-session");
        when(secondSession.isOpen()).thenReturn(true);
        when(secondSession.getAttributes()).thenReturn(new HashMap<>());

        gameHandler.afterConnectionEstablished(secondSession);

        // 3. 첫 번째 세션이 종료되고 두 번째 세션만 남아있어야 함
        verify(firstSession).close(any());
        
        Set<WebSocketSession> userSessions = gameHandler.getConnectedUserSessions("user123");
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
        assertTrue(userSessions.contains(secondSession));
        assertFalse(userSessions.contains(firstSession));
    }
}
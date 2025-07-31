package com.ssafy.chat.common.service;

import com.ssafy.chat.common.dto.AuthRequest;
import com.ssafy.chat.common.dto.AuthResponse;
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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(redisTemplate);
        ReflectionTestUtils.setField(authService, "websocketBaseUrl", "ws://localhost:8084");
        
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("채팅 세션 생성 성공 - Game 채팅")
    void testCreateChatSession_Game_Success() {
        // Given
        AuthRequest request = new AuthRequest();
        request.setAccessToken("valid-jwt-token");
        request.setChatType("game");
        request.setRoomId("team1");

        // When
        AuthResponse response = authService.authenticateAndCreateSession(request);

        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getSessionToken());
        assertTrue(response.getWebsocketUrl().contains("/ws/game-chat"));
        assertTrue(response.getWebsocketUrl().contains("token=" + response.getSessionToken()));
        assertTrue(response.getWebsocketUrl().contains("teamId=team1"));
        assertEquals(3600, response.getExpiresIn());

        // Redis 저장 확인
        verify(hashOperations).putAll(contains("user:session:"), any(Map.class));
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("채팅 세션 생성 성공 - Match 채팅")
    void testCreateChatSession_Match_Success() {
        // Given
        AuthRequest request = new AuthRequest();
        request.setAccessToken("valid-jwt-token");
        request.setChatType("match");
        request.setRoomId("match123");

        // When
        AuthResponse response = authService.authenticateAndCreateSession(request);

        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getSessionToken());
        assertTrue(response.getWebsocketUrl().contains("/ws/match-chat"));
        assertTrue(response.getWebsocketUrl().contains("token=" + response.getSessionToken()));
        assertTrue(response.getWebsocketUrl().contains("teamId=match123"));
    }

    @Test
    @DisplayName("세션 무효화 성공")
    void testInvalidateSession_Success() {
        // Given
        String sessionToken = "test-session-token";
        String expectedKey = "user:session:" + sessionToken;

        // When
        authService.invalidateSession(sessionToken);

        // Then
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("세션 무효화 실패 - Redis 오류")
    void testInvalidateSession_RedisError() {
        // Given
        String sessionToken = "test-session-token";
        doThrow(new RuntimeException("Redis connection failed"))
                .when(redisTemplate).delete(anyString());

        // When & Then - 예외가 발생해도 메서드는 정상 종료
        assertDoesNotThrow(() -> authService.invalidateSession(sessionToken));
    }

    @Test
    @DisplayName("채팅 세션 생성 실패 - Redis 오류")
    void testCreateChatSession_RedisError() {
        // Given
        AuthRequest request = new AuthRequest();
        request.setAccessToken("valid-jwt-token");
        request.setChatType("game");
        request.setRoomId("team1");

        doThrow(new RuntimeException("Redis connection failed"))
                .when(hashOperations).putAll(anyString(), any(Map.class));

        // When
        AuthResponse response = authService.authenticateAndCreateSession(request);

        // Then
        assertFalse(response.isSuccess());
        assertNull(response.getSessionToken());
        assertNull(response.getWebsocketUrl());
        assertTrue(response.getErrorMessage().contains("Internal server error"));
    }

    @Test
    @DisplayName("WebSocket URL 생성 확인 - 다양한 chatType")
    void testWebSocketUrlGeneration() {
        // Game 채팅
        AuthRequest gameRequest = new AuthRequest();
        gameRequest.setChatType("game");
        gameRequest.setRoomId("team1");
        gameRequest.setAccessToken("token");

        AuthResponse gameResponse = authService.authenticateAndCreateSession(gameRequest);
        assertTrue(gameResponse.getWebsocketUrl().contains("/ws/game-chat"));

        // Match 채팅
        AuthRequest matchRequest = new AuthRequest();
        matchRequest.setChatType("match");
        matchRequest.setRoomId("match123");
        matchRequest.setAccessToken("token");

        AuthResponse matchResponse = authService.authenticateAndCreateSession(matchRequest);
        assertTrue(matchResponse.getWebsocketUrl().contains("/ws/match-chat"));
    }

    @Test
    @DisplayName("임시 사용자 정보 생성 확인")
    void testMockUserInfoGeneration() {
        // Given
        AuthRequest request = new AuthRequest();
        request.setAccessToken("test-token");
        request.setChatType("game");
        request.setRoomId("team5");

        // When
        AuthResponse response = authService.authenticateAndCreateSession(request);

        // Then
        assertTrue(response.isSuccess());
        
        // Redis에 저장된 데이터 확인
        verify(hashOperations).putAll(anyString(), argThat(sessionData -> {
            Map<String, Object> data = (Map<String, Object>) sessionData;
            return data.get("userName").equals("임시사용자") &&
                   data.get("userTeamId").equals("team5") &&
                   data.get("chatType").equals("game") &&
                   data.get("roomId").equals("team5") &&
                   data.get("userId").toString().startsWith("temp_user_");
        }));
    }
}
package com.ssafy.chat.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.dto.AuthRequest;
import com.ssafy.chat.common.dto.AuthResponse;
import com.ssafy.chat.common.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("채팅 세션 생성 API 성공")
    void testCreateChatSession_Success() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setAccessToken("valid-jwt-token");
        request.setChatType("game");
        request.setRoomId("team1");

        AuthResponse response = AuthResponse.builder()
                .success(true)
                .sessionToken("session-token-123")
                .websocketUrl("ws://localhost:8084/ws/game-chat?token=session-token-123&teamId=team1")
                .expiresIn(3600)
                .build();

        when(authService.authenticateAndCreateSession(any(AuthRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/chat/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sessionToken").value("session-token-123"))
                .andExpect(jsonPath("$.websocketUrl").value("ws://localhost:8084/ws/game-chat?token=session-token-123&teamId=team1"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authService).authenticateAndCreateSession(any(AuthRequest.class));
    }

    @Test
    @DisplayName("채팅 세션 생성 API 실패 - 인증 실패")
    void testCreateChatSession_AuthFailure() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setAccessToken("invalid-jwt-token");
        request.setChatType("game");
        request.setRoomId("team1");

        AuthResponse response = AuthResponse.builder()
                .success(false)
                .errorMessage("Authentication failed")
                .build();

        when(authService.authenticateAndCreateSession(any(AuthRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/chat/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Authentication failed"));
    }

    @Test
    @DisplayName("채팅 세션 생성 API 실패 - 서버 오류")
    void testCreateChatSession_ServerError() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setAccessToken("valid-jwt-token");
        request.setChatType("game");
        request.setRoomId("team1");

        when(authService.authenticateAndCreateSession(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(post("/api/chat/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Internal server error"));
    }

    @Test
    @DisplayName("세션 무효화 API 성공")
    void testInvalidateSession_Success() throws Exception {
        // Given
        String sessionToken = "session-token-123";

        // When & Then
        mockMvc.perform(delete("/api/chat/auth/session/" + sessionToken))
                .andExpect(status().isOk());

        verify(authService).invalidateSession(sessionToken);
    }

    @Test
    @DisplayName("세션 무효화 API 실패 - 서버 오류")
    void testInvalidateSession_ServerError() throws Exception {
        // Given
        String sessionToken = "session-token-123";
        doThrow(new RuntimeException("Redis connection failed"))
                .when(authService).invalidateSession(sessionToken);

        // When & Then
        mockMvc.perform(delete("/api/chat/auth/session/" + sessionToken))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("헬스체크 API")
    void testHealthCheck() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/chat/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Chat Auth Service is running"));
    }

    @Test
    @DisplayName("Match 채팅 세션 생성")
    void testCreateMatchChatSession() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setAccessToken("valid-jwt-token");
        request.setChatType("match");
        request.setRoomId("match456");

        AuthResponse response = AuthResponse.builder()
                .success(true)
                .sessionToken("match-session-456")
                .websocketUrl("ws://localhost:8084/ws/match-chat?token=match-session-456&teamId=match456")
                .expiresIn(3600)
                .build();

        when(authService.authenticateAndCreateSession(any(AuthRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/chat/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.websocketUrl").value("ws://localhost:8084/ws/match-chat?token=match-session-456&teamId=match456"));
    }

    @Test
    @DisplayName("잘못된 요청 형식")
    void testCreateChatSession_InvalidRequest() throws Exception {
        // Given - 잘못된 JSON
        String invalidJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/api/chat/auth/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
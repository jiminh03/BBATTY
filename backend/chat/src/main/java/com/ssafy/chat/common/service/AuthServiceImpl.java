package com.ssafy.chat.common.service;

import com.ssafy.chat.common.dto.AuthRequest;
import com.ssafy.chat.common.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 인증 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final RedisTemplate<String, Object> redisTemplate;

//    @Value("${websocket.base.url:http://i13a403.p.ssafy.io:8084}")
    @Value("${websocket.base.url:ws://localhost:8084}")
    private String websocketBaseUrl;

    private static final String SESSION_KEY_PREFIX = "user:session:";
    private static final int SESSION_EXPIRE_MINUTES = 60; // 1시간

    @Override
    public AuthResponse authenticateAndCreateSession(AuthRequest authRequest) {
        try {
            // Main 서버에서 인증 확인 (임시로 바로 허용)
            Map<String, Object> userInfo = validateTokenWithMainServer(authRequest);
            if (userInfo == null) {
                return AuthResponse.builder()
                        .success(false)
                        .errorMessage("Authentication failed")
                        .build();
            }

            // 세션 토큰 생성
            String sessionToken = generateSessionToken();

            // Redis에 세션 정보 저장
            saveSessionToRedis(sessionToken, userInfo, authRequest);

            // WebSocket URL 생성
            String websocketUrl = buildWebSocketUrl(authRequest.getChatType(), sessionToken, authRequest.getRoomId());

            return AuthResponse.builder()
                    .success(true)
                    .sessionToken(sessionToken)
                    .websocketUrl(websocketUrl)
                    .expiresIn(SESSION_EXPIRE_MINUTES * 60)
                    .build();

        } catch (Exception e) {
            log.error("인증 및 세션 생성 실패", e);
            return AuthResponse.builder()
                    .success(false)
                    .errorMessage("Internal server error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public void invalidateSession(String sessionToken) {
        try {
            String sessionKey = SESSION_KEY_PREFIX + sessionToken;
            redisTemplate.delete(sessionKey);
            log.info("세션 무효화 완료 - token: {}", sessionToken);
        } catch (Exception e) {
            log.error("세션 무효화 실패 - token: {}", sessionToken, e);
        }
    }

    /**
     * Main 서버에서 토큰 검증 및 사용자 정보 조회
     * TODO: 나중에 Kafka로 비동기 통신 구현 예정
     */
    private Map<String, Object> validateTokenWithMainServer(AuthRequest authRequest) {
        // TODO: Main 서버 인증 로직 - Kafka로 변경 예정
        /*
        try {
            String url = mainServerUrl + "/api/auth/validate-chat";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authRequest.getAccessToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chatType", authRequest.getChatType());
            requestBody.put("roomId", authRequest.getRoomId());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (Boolean.TRUE.equals(body.get("success"))) {
                    return (Map<String, Object>) body.get("userInfo");
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Main 서버 인증 호출 실패", e);
            return null;
        }
        */
        
        // 임시: 바로 허용 (개발용)
        log.info("임시 인증 허용 - chatType: {}, roomId: {}", authRequest.getChatType(), authRequest.getRoomId());
        
        Map<String, Object> mockUserInfo = new HashMap<>();
        mockUserInfo.put("userId", "temp_user_" + System.currentTimeMillis());
        mockUserInfo.put("userName", "임시사용자");
        mockUserInfo.put("userTeamId", authRequest.getRoomId()); // roomId를 userTeamId로 사용
        
        return mockUserInfo;
    }

    /**
     * 세션 토큰 생성
     */
    private String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Redis에 세션 정보 저장
     */
    private void saveSessionToRedis(String sessionToken, Map<String, Object> userInfo, AuthRequest authRequest) {
        String sessionKey = SESSION_KEY_PREFIX + sessionToken;

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", userInfo.get("userId"));
        sessionData.put("userName", userInfo.get("userName"));
        sessionData.put("userTeamId", userInfo.get("userTeamId"));
        sessionData.put("chatType", authRequest.getChatType());
        sessionData.put("roomId", authRequest.getRoomId());
        sessionData.put("createdAt", System.currentTimeMillis());

        redisTemplate.opsForHash().putAll(sessionKey, sessionData);
        redisTemplate.expire(sessionKey, SESSION_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("세션 저장 완료 - userId: {}, token: {}", userInfo.get("userId"), sessionToken);
    }

    /**
     * WebSocket URL 생성 (SockJS용)
     */
    private String buildWebSocketUrl(String chatType, String sessionToken, String roomId) {
        String endpoint = "game".equals(chatType) ? "/ws/game-chat" : "/ws/match-chat";
        // SockJS WebSocket 엔드포인트는 /websocket을 추가
        return String.format("%s%s/websocket?token=%s&teamId=%s",
                websocketBaseUrl, endpoint, sessionToken, roomId);
    }
}
package com.ssafy.bbatty.global.config;

import com.ssafy.bbatty.domain.chat.game.handler.GameChatWebSocketHandler;
import com.ssafy.bbatty.domain.chat.match.handler.MatchChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * 전역 WebSocket 설정
 * 채팅 관련 WebSocket 핸들러들을 등록하고 기본 보안 설정을 제공
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameChatWebSocketHandler gameChatWebSocketHandler;
    private final MatchChatWebSocketHandler matchChatWebSocketHandler;

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${websocket.game-chat.path:/ws/game-chat}")
    private String gameChatPath;

    @Value("${websocket.match-chat.path:/ws/match-chat}")
    private String matchChatPath;

    @Value("${websocket.sockjs.enabled:false}")
    private boolean sockJsEnabled;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        // 게임 채팅 WebSocket 핸들러 등록
        var gameChatRegistration = registry.addHandler(gameChatWebSocketHandler, gameChatPath)
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .addInterceptors(new ChatHandshakeInterceptor("game"));

        // 매칭 채팅 WebSocket 핸들러 등록
        var matchChatRegistration = registry.addHandler(matchChatWebSocketHandler, matchChatPath)
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .addInterceptors(new ChatHandshakeInterceptor("match"));

        // SockJS 지원 (설정에 따라)
        if (sockJsEnabled) {
            gameChatRegistration.withSockJS();
            matchChatRegistration.withSockJS();
            log.info("SockJS 폴백 지원 활성화");
        }

        log.info("WebSocket 핸들러 등록 완료 - 게임: {}, 매칭: {}", gameChatPath, matchChatPath);
    }

    /**
     * 채팅 핸드셰이크 인터셉터
     * 연결 전 기본적인 검증 수행
     */
    public static class ChatHandshakeInterceptor implements HandshakeInterceptor {

        private final String chatType;

        public ChatHandshakeInterceptor(String chatType) {
            this.chatType = chatType;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) throws Exception {
            try {
                URI uri = request.getURI();
                String query = uri.getQuery();

                log.debug("{} 채팅 핸드셰이크 시작 - URI: {}", chatType, uri);

                // 쿼리 파라미터 파싱
                Map<String, String> params = parseQueryParams(query);

                // 기본 파라미터 검증
                if (!validateBasicParams(params, chatType)) {
                    log.warn("{} 채팅 연결 거부 - 파라미터 검증 실패: {}", chatType, uri);
                    return false;
                }

                // WebSocket 세션 속성에 파라미터 저장
                params.forEach(attributes::put);
                attributes.put("chatType", chatType);
                attributes.put("connectTime", System.currentTimeMillis());

                // 클라이언트 IP 저장 (로깅/보안용)
                String clientIp = getClientIpAddress(request);
                attributes.put("clientIp", clientIp);

                log.info("{} 채팅 핸드셰이크 성공 - IP: {}, params: {}", chatType, clientIp, params);
                return true;

            } catch (Exception e) {
                log.error("{} 채팅 핸드셰이크 오류", chatType, e);
                return false;
            }
        }

        @Override
        public void afterHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Exception exception) {
            if (exception != null) {
                log.error("{} 채팅 핸드셰이크 후 오류", chatType, exception);
            } else {
                log.debug("{} 채팅 핸드셰이크 완료", chatType);
            }
        }

        /**
         * 쿼리 파라미터 파싱
         */
        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new java.util.HashMap<>();

            if (query != null && !query.isEmpty()) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2) {
                        try {
                            String key = java.net.URLDecoder.decode(keyValue[0], "UTF-8");
                            String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                            params.put(key, value);
                        } catch (Exception e) {
                            log.warn("쿼리 파라미터 디코딩 실패: {}", pair, e);
                        }
                    }
                }
            }

            return params;
        }

        /**
         * 기본 파라미터 검증
         */
        private boolean validateBasicParams(Map<String, String> params, String chatType) {
            // 사용자 ID는 필수
            String userId = params.get("userId");
            if (userId == null || userId.trim().isEmpty()) {
                return false;
            }

            // 게임 채팅은 teamId 필수
            if ("game".equals(chatType)) {
                String teamId = params.get("teamId");
                if (teamId == null || teamId.trim().isEmpty()) {
                    return false;
                }
            }

            // 추가 검증 로직 (필요시)
            // - 토큰 유효성 검사
            // - 사용자 권한 확인
            // - Rate limiting 등

            return true;
        }

        /**
         * 클라이언트 IP 주소 추출
         */
        private String getClientIpAddress(ServerHttpRequest request) {
            String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddress() != null ?
                    request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }
    }
}
package com.ssafy.bbatty.global.config;

import com.ssafy.bbatty.domain.chat.common.dto.ChatSession;
import com.ssafy.bbatty.domain.chat.game.handler.GameChatWebSocketHandler;
//import com.ssafy.bbatty.domain.chat.match.handler.MatchChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;
//    private final MatchChatWebSocketHandler matchChatWebSocketHandler;

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
//        var matchChatRegistration = registry.addHandler(matchChatWebSocketHandler, matchChatPath)
//                .setAllowedOriginPatterns(allowedOrigins.split(","))
//                .addInterceptors(new ChatHandshakeInterceptor("match"));

//        // SockJS 지원 (설정에 따라)
//        if (sockJsEnabled) {
//            gameChatRegistration.withSockJS();
//            matchChatRegistration.withSockJS();
//            log.info("SockJS 폴백 지원 활성화");
//        }

        log.info("WebSocket 핸들러 등록 완료 - 게임: {}, 매칭: {}", gameChatPath, matchChatPath);
    }

    /**
     * 채팅 핸드셰이크 인터셉터
     * 연결 전 기본적인 검증 수행
     */
    public class ChatHandshakeInterceptor implements HandshakeInterceptor {

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
         * 기본 파라미터 검증 - 세션 토큰 기반
         */
        private boolean validateBasicParams(Map<String, String> params, String chatType) {
            String sessionToken = params.get("session");
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                log.warn("세션 토큰이 없습니다.");
                return false;
            }

            try {
                // Redis에서 세션 검증
                Object sessionObj = redisTemplate.opsForValue().get("chat_session:" + sessionToken);
                if (sessionObj == null) {
                    log.warn("유효하지 않은 세션 토큰: {}", sessionToken);
                    return false;
                }

                // 세션 정보를 attributes에 저장
                if (sessionObj instanceof ChatSession) {
                    ChatSession session = (ChatSession) sessionObj;
                    if (session.isExpired()) {
                        log.warn("만료된 세션 토큰: {}", sessionToken);
                        return false;
                    }

                    // WebSocket attributes에 세션 정보 설정
                    params.put("internalUserId", session.getUserId().toString());
                    params.put("userNickname", session.getUserNickname());
                    params.put("teamId", session.getTeamId());
                    params.put("gameId", session.getGameId().toString());

                    // 일회용 토큰 삭제
                    redisTemplate.delete("chat_session:" + sessionToken);

                    return true;
                }

                return false;

            } catch (Exception e) {
                log.error("세션 토큰 검증 중 오류", e);
                return false;
            }
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
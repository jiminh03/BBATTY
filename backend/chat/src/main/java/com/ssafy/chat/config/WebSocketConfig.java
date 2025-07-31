package com.ssafy.chat.config;

import com.ssafy.chat.watch.handler.GameChatWebSocketHandler;
import com.ssafy.chat.match.handler.MatchChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 설정
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameChatWebSocketHandler gameChatWebSocketHandler;
    private final MatchChatWebSocketHandler matchChatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("WebSocket 핸들러 등록 시작");

        // 경기 직관 채팅 WebSocket 엔드포인트
        registry.addHandler(gameChatWebSocketHandler, "/ws/game-chat")
                .addInterceptors(new WebSocketHandshakeInterceptor()) // 핸드셰이크 인터셉터 추가
                .setAllowedOrigins("*") // CORS 허용
                .withSockJS(); // SockJS 지원

        // 매칭 채팅 WebSocket 엔드포인트  
        registry.addHandler(matchChatWebSocketHandler, "/ws/match-chat")
                .addInterceptors(new WebSocketHandshakeInterceptor()) // 핸드셰이크 인터셉터 추가
                .setAllowedOrigins("*") // CORS 허용
                .withSockJS(); // SockJS 지원

        log.info("WebSocket 핸들러 등록 완료 - /ws/game-chat, /ws/match-chat");
    }
}
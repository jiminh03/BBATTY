package com.ssafy.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 핸드셰이크 인터셉터
 * URL 쿼리 파라미터를 WebSocket 세션 속성으로 전달
 */
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        URI uri = request.getURI();
        log.info("WebSocket 핸드셰이크 요청 - URI: {}", uri);
        
        // 쿼리 파라미터 추출하여 세션 속성에 저장
        Map<String, String> queryParams = UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .toSingleValueMap();
        
        log.info("쿼리 파라미터: {}", queryParams);
        
        // 모든 쿼리 파라미터를 세션 속성으로 저장
        attributes.putAll(queryParams);
        
        // 필수 파라미터 검증
        String token = queryParams.get("token");
        if (token == null || token.trim().isEmpty()) {
            log.warn("필수 파라미터 누락: token");
            return false;
        }
        
        log.info("핸드셰이크 성공 - token: {}", token);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("핸드셰이크 후 처리 중 오류 발생", exception);
        } else {
            log.info("핸드셰이크 완료");
        }
    }
}
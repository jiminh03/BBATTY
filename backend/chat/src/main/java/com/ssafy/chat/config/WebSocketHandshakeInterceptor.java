package com.ssafy.chat.config;

import com.ssafy.chat.match.service.MatchChatAuthService;
import com.ssafy.chat.watch.service.WatchChatAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 핸드셰이크 인터셉터
 * 세션 토큰으로 Redis에서 사용자 정보 조회 후 WebSocket 세션 속성에 저장
 * 매칭 채팅과 직관 채팅 모두 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final MatchChatAuthService matchChatAuthService;
    private final WatchChatAuthService watchChatAuthService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        URI uri = request.getURI();
        log.info("WebSocket 핸드셰이크 요청 - URI: {}", uri);
        
        try {
            // 쿼리 파라미터 추출
            Map<String, String> queryParams = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();
            
            log.info("쿼리 파라미터: {}", queryParams);
            
            // 필수 파라미터 검증
            String sessionToken = queryParams.get("token");
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                log.warn("필수 파라미터 누락: token");
                return false;
            }
            
            // 채팅 타입 구분 (matchId 있으면 매칭채팅, gameId 있으면 직관채팅)
            String matchId = queryParams.get("matchId");
            String gameId = queryParams.get("gameId");
            
            if (matchId != null && !matchId.trim().isEmpty()) {
                // 매칭 채팅 처리
                return handleMatchChat(sessionToken, matchId, attributes);
            } else if (gameId != null && !gameId.trim().isEmpty()) {
                // 직관 채팅 처리 (무명)
                return handleWatchChat(sessionToken, gameId, attributes);
            } else {
                log.warn("채팅 타입 식별 불가 - matchId와 gameId 모두 누락");
                return false;
            }
            
        } catch (SecurityException e) {
            log.warn("세션 토큰 검증 실패: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("핸드셰이크 처리 중 오류 발생", e);
            return false;
        }
    }
    
    private boolean handleMatchChat(String sessionToken, String matchId, Map<String, Object> attributes) {
        try {
            // Redis에서 세션 토큰으로 사용자 정보 조회
            Map<String, Object> userInfo = matchChatAuthService.getUserInfoByToken(sessionToken);
            
            // 세션의 matchId와 쿼리 파라미터의 matchId 일치 확인
            String sessionMatchId = (String) userInfo.get("matchId");
            if (!matchId.equals(sessionMatchId)) {
                log.warn("매칭 ID 불일치 - query: {}, session: {}", matchId, sessionMatchId);
                return false;
            }
            
            // WebSocket 세션 속성에 사용자 정보 저장 (개인화된 채팅)
            attributes.put("chatType", "match");
            attributes.put("userId", userInfo.get("userId"));
            attributes.put("userName", userInfo.get("userName"));
            attributes.put("matchId", userInfo.get("matchId"));
            attributes.put("nickname", userInfo.get("nickname"));
            attributes.put("winRate", userInfo.get("winRate"));
            attributes.put("profileImgUrl", userInfo.get("profileImgUrl"));
            attributes.put("isVictoryFairy", userInfo.get("isVictoryFairy"));
            attributes.put("gender", userInfo.get("gender"));
            attributes.put("age", userInfo.get("age"));
            attributes.put("teamId", userInfo.get("teamId"));
            
            log.info("매칭 채팅 핸드셰이크 성공 - userId: {}, matchId: {}, nickname: {}", 
                    userInfo.get("userId"), userInfo.get("matchId"), userInfo.get("nickname"));
            
            return true;
            
        } catch (Exception e) {
            log.error("매칭 채팅 핸드셰이크 실패", e);
            return false;
        }
    }
    
    private boolean handleWatchChat(String sessionToken, String gameId, Map<String, Object> attributes) {
        try {
            // Redis에서 세션 토큰으로 무명 사용자 정보 조회
            Map<String, Object> userInfo = watchChatAuthService.getUserInfoByToken(sessionToken);
            
            // 세션의 gameId와 쿼리 파라미터의 gameId 일치 확인
            String sessionGameId = (String) userInfo.get("gameId");
            if (!gameId.equals(sessionGameId)) {
                log.warn("게임 ID 불일치 - query: {}, session: {}", gameId, sessionGameId);
                return false;
            }
            
            // WebSocket 세션 속성에 무명 정보만 저장 (완전 익명 채팅)
            attributes.put("chatType", "watch");
            attributes.put("teamId", userInfo.get("teamId"));
            attributes.put("gameId", userInfo.get("gameId"));
            attributes.put("isAttendanceVerified", userInfo.get("isAttendanceVerified"));
            
            log.info("직관 채팅 핸드셰이크 성공 - teamId: {}, gameId: {}", 
                    userInfo.get("teamId"), userInfo.get("gameId"));
            
            return true;
            
        } catch (Exception e) {
            log.error("직관 채팅 핸드셰이크 실패", e);
            return false;
        }
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
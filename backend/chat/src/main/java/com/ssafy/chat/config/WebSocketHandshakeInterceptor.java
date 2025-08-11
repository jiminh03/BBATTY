package com.ssafy.chat.config;

import com.ssafy.chat.match.service.MatchChatRoomAuthService;
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

    private final MatchChatRoomAuthService matchChatRoomAuthService;
    private final WatchChatAuthService watchChatAuthService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        URI uri = request.getURI();
        log.debug("WebSocket 핸드셰이크 요청 - URI: {}", uri);
        
        try {
            // 쿼리 파라미터 추출
            Map<String, String> queryParams = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();
            
            log.debug("쿼리 파라미터: {}", queryParams);
            
            // 필수 파라미터 검증
            String sessionToken = queryParams.get("sessionToken");
            if (sessionToken == null || sessionToken.trim().isEmpty()) {
                log.warn("필수 파라미터 누락: sessionToken");
                return false;
            }
            
            // 채팅 타입 구분 (matchId 있으면 매칭채팅, gameId 있으면 직관채팅)
            String matchId = queryParams.get("matchId");
            String gameId = queryParams.get("gameId");
            String teamId = queryParams.get("teamId");
            
            if (matchId != null && !matchId.trim().isEmpty()) {
                // 매칭 채팅 처리
                return handleMatchChat(sessionToken, matchId, attributes);
            } else if (gameId != null && !gameId.trim().isEmpty() && teamId != null && !teamId.trim().isEmpty()) {
                // 직관 채팅 처리 (무명)
                return handleWatchChat(sessionToken, gameId, teamId, attributes);
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
            Map<String, Object> userInfo = matchChatRoomAuthService.getUserInfoByToken(sessionToken);

            // ✅ 조회된 userInfo 내용 확인
            log.debug("조회된 userInfo: {}", userInfo);

            // WebSocket 세션 속성에 사용자 정보 저장
            attributes.put("chatType", "match");
            attributes.put("userId", userInfo.get("userId"));
            attributes.put("matchId", matchId);  // 매개변수로 받은 matchId 직접 사용
            attributes.put("nickname", userInfo.get("nickname"));
            attributes.put("winRate", userInfo.get("winRate"));
            attributes.put("profileImgUrl", userInfo.get("profileImgUrl"));
            attributes.put("isWinFairy", userInfo.get("isWinFairy"));
            attributes.put("gender", userInfo.get("gender"));
            attributes.put("age", userInfo.get("age"));
            attributes.put("teamId", userInfo.get("teamId"));

            // ✅ 설정된 attributes 확인 (핵심 정보만)
            log.debug("설정된 핵심 attributes - userId: '{}', nickname: '{}', matchId: '{}'",
                    attributes.get("userId"), attributes.get("nickname"), attributes.get("matchId"));

            // 디버깅 정보 - 프로덕션에서는 제거됨

            log.info("매칭 채팅 핸드셰이크 성공 - userId: {}, matchId: {}, nickname: {}",
                    userInfo.get("userId"), matchId, userInfo.get("nickname"));

            return true;

        } catch (Exception e) {
            log.error("매칭 채팅 핸드셰이크 실패", e);
            return false;
        }
    }



    private boolean handleWatchChat(String sessionToken, String gameId, String teamId, Map<String, Object> attributes) {
        try {
            // 세션 토큰 유효성만 검증 (Redis 조회 최소화)
            Map<String, Object> userInfo = watchChatAuthService.getUserInfoByToken(sessionToken);
            
            log.debug("직관 채팅 userInfo 조회 결과: {}", userInfo);
            
            // WebSocket 세션 속성에 사용자 정보 저장 (URL에서 받은 teamId, gameId 사용)
            attributes.put("chatType", "watch");
            attributes.put("userId", userInfo.get("userId"));
            attributes.put("teamId", Long.parseLong(teamId));  // URL에서 받은 teamId 사용
            attributes.put("gameId", Long.parseLong(gameId));  // URL에서 받은 gameId 사용
            attributes.put("isAttendanceVerified", userInfo.get("isAttendanceVerified"));
            
            log.debug("직관 채팅 attributes 설정 완료: chatType={}, userId={}, teamId={}, gameId={}", 
                attributes.get("chatType"), attributes.get("userId"), attributes.get("teamId"), attributes.get("gameId"));
            
            log.info("직관 채팅 핸드셰이크 성공 - teamId: {}, gameId: {}", 
                    teamId, gameId);
            
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
            log.debug("핸드셰이크 완료");
        }
    }
}
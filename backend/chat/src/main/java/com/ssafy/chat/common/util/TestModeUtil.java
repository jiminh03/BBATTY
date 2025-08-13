package com.ssafy.chat.common.util;

import com.ssafy.chat.common.dto.SessionTokenInfo;
import com.ssafy.chat.config.ChatConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Map;

/**
 * 🧪 테스트 모드 유틸리티
 * 전역 설정으로 테스트 모드 활성화 시 인증 우회 및 더미 데이터 생성
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestModeUtil {
    
    private final ChatConfiguration chatConfiguration;
    
    /**
     * 테스트 모드가 활성화되어 있고 세션 토큰이 "test-" 로 시작하는지 확인
     */
    public boolean isTestMode(String sessionToken) {
        return chatConfiguration.isTestModeEnabled() && 
               sessionToken != null && 
               sessionToken.startsWith("test-");
    }
    
    /**
     * 테스트용 SessionTokenInfo 생성
     */
    public SessionTokenInfo createTestTokenInfo(String sessionToken, WebSocketSession session) {
        if (!isTestMode(sessionToken)) {
            return null;
        }
        
        try {
            URI uri = session.getUri();
            Map<String, String> queryParams = extractQueryParams(uri);
            
            // 매칭 채팅 테스트 모드
            String matchId = queryParams.get("matchId");
            if (matchId != null && !matchId.trim().isEmpty()) {
                return createMatchTestToken(sessionToken, matchId);
            }
            
            // 직관 채팅 테스트 모드
            String gameId = queryParams.get("gameId");
            String teamId = queryParams.get("teamId");
            if (gameId != null && teamId != null) {
                return createWatchTestToken(sessionToken, gameId, teamId);
            }
            
            // 기본 테스트 모드
            return createBasicTestToken(sessionToken);
            
        } catch (Exception e) {
            log.error("🚨 테스트 토큰 생성 실패", e);
            return null;
        }
    }
    
    /**
     * 매칭 채팅 테스트 토큰 생성
     */
    private SessionTokenInfo createMatchTestToken(String sessionToken, String matchId) {
        log.info("🧪 매칭 채팅 테스트 토큰 생성 - matchId: {}", matchId);
        
        return SessionTokenInfo.builder()
                .token(sessionToken)
                .userId(99999L)
                .nickname("매칭테스터" + System.currentTimeMillis() % 1000)
                .roomId(matchId)
                .roomType("MATCH")
                .gameId(null)
                .teamId(1L)
                .teamName("테스트팀")
                .profileImgUrl("https://example.com/test-profile.jpg")
                .isWinFairy(false)
                .issuedAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + 3600000) // 1시간 후 만료
                .build();
    }
    
    /**
     * 직관 채팅 테스트 토큰 생성
     */
    private SessionTokenInfo createWatchTestToken(String sessionToken, String gameId, String teamId) {
        log.info("🧪 직관 채팅 테스트 토큰 생성 - gameId: {}, teamId: {}", gameId, teamId);
        
        return SessionTokenInfo.builder()
                .token(sessionToken)
                .userId(88888L)
                .nickname("직관테스터" + System.currentTimeMillis() % 1000)
                .roomId("watch_" + gameId + "_" + teamId)
                .roomType("WATCH")
                .gameId(Long.parseLong(gameId))
                .teamId(Long.parseLong(teamId))
                .teamName("응원팀" + teamId)
                .issuedAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + 3600000) // 1시간 후 만료
                .build();
    }
    
    /**
     * 기본 테스트 토큰 생성
     */
    private SessionTokenInfo createBasicTestToken(String sessionToken) {
        log.info("🧪 기본 테스트 토큰 생성");
        
        return SessionTokenInfo.builder()
                .token(sessionToken)
                .userId(77777L)
                .nickname("기본테스터")
                .roomId("test_room")
                .roomType("TEST")
                .issuedAt(System.currentTimeMillis())
                .expiresAt(System.currentTimeMillis() + 3600000) // 1시간 후 만료
                .build();
    }
    
    /**
     * URI에서 쿼리 파라미터 추출
     */
    private Map<String, String> extractQueryParams(URI uri) {
        Map<String, String> params = new java.util.HashMap<>();
        
        if (uri != null && uri.getQuery() != null) {
            String[] pairs = uri.getQuery().split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        
        return params;
    }
    
    /**
     * 테스트 모드 상태 로깅
     */
    public void logTestModeStatus() {
        if (chatConfiguration.isTestModeEnabled()) {
            log.warn("🧪 WARNING: 테스트 모드가 활성화되어 있습니다! 프로덕션에서는 비활성화하세요.");
        } else {
            log.info("🔒 테스트 모드가 비활성화되어 있습니다.");
        }
    }
}
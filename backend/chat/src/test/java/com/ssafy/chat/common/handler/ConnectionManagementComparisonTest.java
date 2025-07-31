package com.ssafy.chat.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.common.service.RedisPubSubService;
import com.ssafy.chat.match.handler.MatchChatWebSocketHandler;
import com.ssafy.chat.watch.handler.GameChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Match vs Watch 연결 관리 차이점 비교 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
class ConnectionManagementComparisonTest {

    @Mock
    private RedisPubSubService redisPubSubService;

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @Mock
    private WebSocketSession session3;

    private MatchChatWebSocketHandler matchHandler;
    private GameChatWebSocketHandler gameHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        matchHandler = new MatchChatWebSocketHandler(objectMapper, redisPubSubService);
        gameHandler = new GameChatWebSocketHandler(objectMapper, redisPubSubService);
    }

    @Test
    @DisplayName("Match: 같은 사용자의 다중 세션 허용")
    void testMatchAllowsMultipleSessions() throws Exception {
        String userId = "user1";
        String userName = "testUser";
        String matchId = "match123";

        setupMatchSession(session1, userId, userName, matchId, "session-1");
        setupMatchSession(session2, userId, userName, matchId, "session-2");
        setupMatchSession(session3, userId, userName, matchId, "session-3");

        matchHandler.afterConnectionEstablished(session1);
        matchHandler.afterConnectionEstablished(session2);
        matchHandler.afterConnectionEstablished(session3);

        Set<WebSocketSession> userSessions = matchHandler.getConnectedUserSessions(userId);
        assertNotNull(userSessions);
        assertEquals(3, userSessions.size());
        assertTrue(userSessions.contains(session1));
        assertTrue(userSessions.contains(session2));
        assertTrue(userSessions.contains(session3));

        verify(session1, never()).close(any(CloseStatus.class));
        verify(session2, never()).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("Watch: 같은 사용자의 단일 세션만 허용 (기존 연결 해제)")
    void testGameAllowsSingleSessionOnly() throws Exception {
        String userId = "user1";
        String userName = "testUser";
        String teamId = "team1";

        setupGameSession(session1, userId, userName, teamId, teamId, "session-1");
        setupGameSession(session2, userId, userName, teamId, teamId, "session-2");
        setupGameSession(session3, userId, userName, teamId, teamId, "session-3");

        gameHandler.afterConnectionEstablished(session1);

        Set<WebSocketSession> userSessions = safeGetSessions(gameHandler, userId);
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
        assertTrue(userSessions.contains(session1));

        gameHandler.afterConnectionEstablished(session2);

        verify(session1).close(any(CloseStatus.class));
        userSessions = safeGetSessions(gameHandler, userId);
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
        assertTrue(userSessions.contains(session2));
        assertFalse(userSessions.contains(session1));

        gameHandler.afterConnectionEstablished(session3);

        verify(session2).close(any(CloseStatus.class));
        userSessions = safeGetSessions(gameHandler, userId);
        assertNotNull(userSessions);
        assertEquals(1, userSessions.size());
        assertTrue(userSessions.contains(session3));
        assertFalse(userSessions.contains(session2));
    }

    @Test
    @DisplayName("Match vs Watch: 서로 다른 사용자는 동시 연결 가능")
    void testDifferentUsersCanConnectSimultaneously() throws Exception {
        String user1Id = "user1";
        String user2Id = "user2";
        String userName1 = "testUser1";
        String userName2 = "testUser2";
        String roomId = "room123";

        // Match 테스트 - matchId 키 사용
        setupMatchSession(session1, user1Id, userName1, roomId, "match-session-1");
        setupMatchSession(session2, user2Id, userName2, roomId, "match-session-2");

        // Match Handler 테스트
        matchHandler.afterConnectionEstablished(session1);
        matchHandler.afterConnectionEstablished(session2);

        Set<WebSocketSession> matchSessionsUser1 = safeGetSessions(matchHandler, user1Id);
        Set<WebSocketSession> matchSessionsUser2 = safeGetSessions(matchHandler, user2Id);

        System.out.println("Match - User1 sessions: " + matchSessionsUser1.size());
        System.out.println("Match - User2 sessions: " + matchSessionsUser2.size());

        assertEquals(1, matchSessionsUser1.size(), "Match - User1은 1개 세션을 가져야 함");
        assertEquals(1, matchSessionsUser2.size(), "Match - User2는 1개 세션을 가져야 함");

        // 새로운 세션 객체로 Watch 테스트
        WebSocketSession gameSession1 = mock(WebSocketSession.class);
        WebSocketSession gameSession2 = mock(WebSocketSession.class);

        setupGameSession(gameSession1, user1Id, userName1, roomId, roomId, "game-session-1");
        setupGameSession(gameSession2, user2Id, userName2, roomId, roomId, "game-session-2");

        // Game Handler 테스트
        gameHandler.afterConnectionEstablished(gameSession1);
        gameHandler.afterConnectionEstablished(gameSession2);

        Set<WebSocketSession> gameSessionsUser1 = safeGetSessions(gameHandler, user1Id);
        Set<WebSocketSession> gameSessionsUser2 = safeGetSessions(gameHandler, user2Id);

        System.out.println("Game - User1 sessions: " + gameSessionsUser1.size());
        System.out.println("Game - User2 sessions: " + gameSessionsUser2.size());

        assertEquals(1, gameSessionsUser1.size(), "Game - User1은 1개 세션을 가져야 함");
        assertEquals(1, gameSessionsUser2.size(), "Game - User2는 1개 세션을 가져야 함");

        // 모든 세션이 닫히지 않았는지 확인
        verify(session1, never()).close(any(CloseStatus.class));
        verify(session2, never()).close(any(CloseStatus.class));
        verify(gameSession1, never()).close(any(CloseStatus.class));
        verify(gameSession2, never()).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("연결 관리 통계 비교")
    void testConnectionStats() throws Exception {
        String user1Id = "user1";
        String user2Id = "user2";
        String userName = "testUser";
        String roomId = "room123";

        setupMatchSession(session1, user1Id, userName, roomId, "session-1");
        setupMatchSession(session2, user1Id, userName, roomId, "session-2");
        setupMatchSession(session3, user2Id, userName, roomId, "session-3");

        matchHandler.afterConnectionEstablished(session1);
        matchHandler.afterConnectionEstablished(session2);
        matchHandler.afterConnectionEstablished(session3);

        Map<String, Object> matchStats = matchHandler.getConnectionStats();
        assertEquals(2, matchStats.get("totalUsers"));
        assertEquals(3, matchStats.get("totalSessions"));

        setupGameSession(session1, user1Id, userName, roomId, roomId, "session-1");
        setupGameSession(session2, user1Id, userName, roomId, roomId, "session-2");
        setupGameSession(session3, user2Id, userName, roomId, roomId, "session-3");

        gameHandler.afterConnectionEstablished(session1);
        gameHandler.afterConnectionEstablished(session2);
        gameHandler.afterConnectionEstablished(session3);

        Map<String, Object> gameStats = gameHandler.getConnectionStats();
        assertEquals(2, gameStats.get("totalUsers"));
        assertEquals(2, gameStats.get("totalSessions")); // 단일 세션 정책 반영
    }

    private void setupMatchSession(WebSocketSession session, String userId, String userName, String matchId, String sessionId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("userName", userName);
        attributes.put("matchId", matchId); // 필드명과 값이 유효해야 함
        attributes.put("profileImgUrl", "https://example.com/profile.jpg");
        attributes.put("isVictoryFairy", false);

        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
    }


    private void setupGameSession(WebSocketSession session, String userId, String userName, String teamId, String userTeamId, String sessionId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("userName", userName);
        attributes.put("teamId", teamId);
        attributes.put("userTeamId", userTeamId);

        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
    }

    /**
     * getConnectedUserSessions 호출 결과가 null일 수도 있으므로 안전하게 null 체크 포함
     */
    private Set<WebSocketSession> safeGetSessions(Object handler, String userId) {
        Set<WebSocketSession> sessions = null;
        if (handler instanceof MatchChatWebSocketHandler) {
            sessions = ((MatchChatWebSocketHandler) handler).getConnectedUserSessions(userId);
        } else if (handler instanceof GameChatWebSocketHandler) {
            sessions = ((GameChatWebSocketHandler) handler).getConnectedUserSessions(userId);
        }
        return sessions != null ? sessions : Collections.emptySet();
    }
}

package com.ssafy.chat.match.service;

import com.ssafy.chat.auth.kafka.ChatAuthRequestProducer;
import com.ssafy.chat.auth.service.ChatAuthResultService;
import com.ssafy.chat.config.JwtProvider;
import com.ssafy.chat.common.util.RedisUtil;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.dto.MatchChatJoinRequest;
import com.ssafy.chat.match.dto.MatchChatRoomCreateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchChatAuthService 테스트")
class MatchChatAuthServiceTest {

    @InjectMocks
    private MatchChatAuthServiceImpl matchChatAuthService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private MatchChatRoomService matchChatRoomService;

    @Mock
    private ChatAuthRequestProducer chatAuthRequestProducer;

    @Mock
    private ChatAuthResultService chatAuthResultService;

    private MatchChatJoinRequest validJoinRequest;
    private MatchChatRoomCreateResponse existingRoom;

    @BeforeEach
    void setUp() {
        validJoinRequest = MatchChatJoinRequest.builder()
                .matchId(1L)
                .nickname("testUser")
                .winRate(75)
                .profileImgUrl("https://example.com/profile.jpg")
                .isWinFairy(true)
                .build();

        existingRoom = MatchChatRoomCreateResponse.builder()
                .matchId("match_1")
                .gameId(1L)
                .matchTitle("테스트 매칭방")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("매칭 채팅 입장 세션 생성 성공")
    void validateAndCreateSession_Success() {
        // given
        String jwtToken = "valid.jwt.token";
        String requestId = "test-request-id";

        // 매칭 채팅방 존재 확인 mock
        given(matchChatRoomService.getMatchChatRoom("1")).willReturn(existingRoom);

        // 인증 요청 mock
        given(chatAuthRequestProducer.sendAuthRequest(
                eq(jwtToken), eq("MATCH"), eq("JOIN"), eq(1L), any(Map.class), eq("testUser")
        )).willReturn(requestId);

        // 인증 결과 mock
        Map<String, Object> authResult = new HashMap<>();
        authResult.put("success", true);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", "123");
        userInfo.put("nickname", "testUser");
        userInfo.put("teamId", "10");
        userInfo.put("teamName", "테스트팀");
        userInfo.put("age", 25);
        userInfo.put("gender", "MALE");
        authResult.put("userInfo", userInfo);

        given(chatAuthResultService.waitForAuthResult(requestId, 10000)).willReturn(authResult);

        // when
        Map<String, Object> response = matchChatAuthService.validateAndCreateSession(jwtToken, validJoinRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.get("sessionToken")).isNotNull();
        assertThat(response.get("userId")).isEqualTo("123");
        assertThat(response.get("nickname")).isEqualTo("testUser");
        assertThat(response.get("matchId")).isEqualTo(1L);

        verify(chatAuthRequestProducer).sendAuthRequest(
                eq(jwtToken), eq("MATCH"), eq("JOIN"), eq(1L), any(Map.class), eq("testUser")
        );
        verify(chatAuthResultService).waitForAuthResult(requestId, 10000);
        verify(redisUtil).setValue(anyString(), any(Map.class), any(Duration.class));
    }

    @Test
    @DisplayName("존재하지 않는 매칭방 입장 시 예외 발생")
    void validateAndCreateSession_RoomNotFound_ThrowsException() {
        // given
        String jwtToken = "valid.jwt.token";
        given(matchChatRoomService.getMatchChatRoom("1")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> matchChatAuthService.validateAndCreateSession(jwtToken, validJoinRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("존재하지 않는 매칭 채팅방입니다");
    }

    @Test
    @DisplayName("비활성 매칭방 입장 시 예외 발생")
    void validateAndCreateSession_InactiveRoom_ThrowsException() {
        // given
        String jwtToken = "valid.jwt.token";
        MatchChatRoomCreateResponse inactiveRoom = MatchChatRoomCreateResponse.builder()
                .matchId("match_1")
                .status("INACTIVE")
                .build();
        given(matchChatRoomService.getMatchChatRoom("1")).willReturn(inactiveRoom);

        // when & then
        assertThatThrownBy(() -> matchChatAuthService.validateAndCreateSession(jwtToken, validJoinRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("비활성 상태의 매칭 채팅방입니다");
    }

    @Test
    @DisplayName("인증 요청 전송 실패 시 예외 발생")
    void validateAndCreateSession_AuthRequestFailed_ThrowsException() {
        // given
        String jwtToken = "valid.jwt.token";
        given(matchChatRoomService.getMatchChatRoom("1")).willReturn(existingRoom);
        given(chatAuthRequestProducer.sendAuthRequest(
                eq(jwtToken), eq("MATCH"), eq("JOIN"), eq(1L), any(Map.class), eq("testUser")
        )).willReturn(null);

        // when & then
        assertThatThrownBy(() -> matchChatAuthService.validateAndCreateSession(jwtToken, validJoinRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("bbatty 서버 인증 요청 전송 실패");
    }

    @Test
    @DisplayName("인증 응답 타임아웃 시 예외 발생")
    void validateAndCreateSession_AuthTimeout_ThrowsException() {
        // given
        String jwtToken = "valid.jwt.token";
        String requestId = "test-request-id";

        given(matchChatRoomService.getMatchChatRoom("1")).willReturn(existingRoom);
        given(chatAuthRequestProducer.sendAuthRequest(
                eq(jwtToken), eq("MATCH"), eq("JOIN"), eq(1L), any(Map.class), eq("testUser")
        )).willReturn(requestId);
        given(chatAuthResultService.waitForAuthResult(requestId, 10000)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> matchChatAuthService.validateAndCreateSession(jwtToken, validJoinRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("인증 응답 타임아웃");
    }

    @Test
    @DisplayName("인증 실패 시 예외 발생")
    void validateAndCreateSession_AuthFailed_ThrowsException() {
        // given
        String jwtToken = "valid.jwt.token";
        String requestId = "test-request-id";

        given(matchChatRoomService.getMatchChatRoom("1")).willReturn(existingRoom);
        given(chatAuthRequestProducer.sendAuthRequest(
                eq(jwtToken), eq("MATCH"), eq("JOIN"), eq(1L), any(Map.class), eq("testUser")
        )).willReturn(requestId);

        Map<String, Object> authResult = new HashMap<>();
        authResult.put("success", false);
        authResult.put("errorMessage", "조건을 만족하지 않습니다");
        given(chatAuthResultService.waitForAuthResult(requestId, 10000)).willReturn(authResult);

        // when & then
        assertThatThrownBy(() -> matchChatAuthService.validateAndCreateSession(jwtToken, validJoinRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("인증 실패: 조건을 만족하지 않습니다");
    }

    @Test
    @DisplayName("세션 토큰으로 사용자 정보 조회 성공")
    void getUserInfoByToken_Success() {
        // given
        String sessionToken = "valid-session-token";
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("userId", "123");
        sessionInfo.put("nickname", "testUser");
        sessionInfo.put("matchId", 1L);

        given(redisUtil.getValue("match_chat_session:" + sessionToken)).willReturn(sessionInfo);

        // when
        Map<String, Object> result = matchChatAuthService.getUserInfoByToken(sessionToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("userId")).isEqualTo("123");
        assertThat(result.get("nickname")).isEqualTo("testUser");
        assertThat(result.get("matchId")).isEqualTo(1L);
    }

    @Test
    @DisplayName("유효하지 않은 세션 토큰으로 조회 시 예외 발생")
    void getUserInfoByToken_InvalidToken_ThrowsException() {
        // given
        String sessionToken = "invalid-session-token";
        given(redisUtil.getValue("match_chat_session:" + sessionToken)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> matchChatAuthService.getUserInfoByToken(sessionToken))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("유효하지 않은 세션 토큰입니다");
    }

    @Test
    @DisplayName("세션 무효화 성공")
    void invalidateSession_Success() {
        // given
        String sessionToken = "valid-session-token";

        // when
        matchChatAuthService.invalidateSession(sessionToken);

        // then
        verify(redisUtil).deleteKey("match_chat_session:" + sessionToken);
    }
}
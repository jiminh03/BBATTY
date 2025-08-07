package com.ssafy.chat.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.match.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchChatRoomService 테스트")
class MatchChatRoomServiceTest {

    @InjectMocks
    private MatchChatRoomServiceImpl matchChatRoomService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GameInfoService gameInfoService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private MatchChatRoomCreateRequest validRequest;
    private GameInfo gameInfo;

    @BeforeEach
    void setUp() {
        validRequest = MatchChatRoomCreateRequest.builder()
                .gameId(1L)
                .matchTitle("테스트 매칭방")
                .matchDescription("테스트용 매칭방입니다")
                .teamId(10L)
                .minAge(20)
                .maxAge(30)
                .genderCondition("ALL")
                .maxParticipants(4)
                .build();

        gameInfo = GameInfo.builder()
                .gameId(1L)
                .homeTeamName("홈팀")
                .awayTeamName("원정팀")
                .dateTime(LocalDateTime.of(2024, 12, 25, 14, 30))
                .stadium("잠실야구장")
                .build();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("매칭 채팅방 생성 성공")
    void createMatchChatRoom_Success() throws Exception {
        // given
        String matchRoomJson = "{\"matchId\":\"match_1\",\"gameId\":1}";
        
        // GameInfoService mock
        when(gameInfoService.getGameInfosByDate(anyString())).thenReturn(Arrays.asList(gameInfo));
        when(objectMapper.writeValueAsString(any(MatchChatRoom.class))).thenReturn(matchRoomJson);

        // when
        MatchChatRoomCreateResponse response = matchChatRoomService.createMatchChatRoom(validRequest);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 게임으로 채팅방 생성 시 예외 발생")
    void createMatchChatRoom_GameNotFound_ThrowsException() {
        // given
        when(gameInfoService.getGameInfosByDate(anyString())).thenReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> matchChatRoomService.createMatchChatRoom(validRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("존재하지 않는 경기입니다");
    }

    @Test
    @DisplayName("매칭 채팅방 조회 성공")
    void getMatchChatRoom_Success() throws Exception {
        // given
        String matchId = "match_1";
        String matchRoomJson = "{\"matchId\":\"match_1\",\"gameId\":1}";
        MatchChatRoom mockRoom = MatchChatRoom.builder()
                .matchId(matchId)
                .gameId(1L)
                .matchTitle("테스트 매칭방")
                .status("ACTIVE") // 상태 추가
                .build();
        
        when(valueOperations.get("match_room:" + matchId)).thenReturn(matchRoomJson);
        when(objectMapper.readValue(matchRoomJson, MatchChatRoom.class)).thenReturn(mockRoom);

        // when
        MatchChatRoomCreateResponse response = matchChatRoomService.getMatchChatRoom(matchId);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 매칭 채팅방 조회 시 null 반환")
    void getMatchChatRoom_NotFound_ReturnsNull() {
        // given
        String matchId = "non_existent_match";
        when(valueOperations.get("match_room:" + matchId)).thenReturn(null);

        // when
        MatchChatRoomCreateResponse response = matchChatRoomService.getMatchChatRoom(matchId);

        // then
        assertThat(response).isNull();
    }
}
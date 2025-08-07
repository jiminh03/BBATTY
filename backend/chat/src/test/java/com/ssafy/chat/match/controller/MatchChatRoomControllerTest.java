package com.ssafy.chat.match.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.match.dto.*;
import com.ssafy.chat.match.service.MatchChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchChatRoomController.class)
@DisplayName("MatchChatRoomController 테스트")
class MatchChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchChatRoomService matchChatRoomService;

    @Autowired
    private ObjectMapper objectMapper;

    private MatchChatRoomCreateRequest validCreateRequest;
    private MatchChatRoomCreateResponse createResponse;
    private MatchChatRoomListRequest listRequest;
    private MatchChatRoomListResponse listResponse;

    @BeforeEach
    void setUp() {
        validCreateRequest = MatchChatRoomCreateRequest.builder()
                .gameId(1L)
                .matchTitle("테스트 매칭방")
                .matchDescription("테스트용 매칭방입니다")
                .teamId(10L)
                .minAge(20)
                .maxAge(30)
                .genderCondition("ALL")
                .maxParticipants(4)
                .build();

        createResponse = MatchChatRoomCreateResponse.builder()
                .matchId("match_1")
                .gameId(1L)
                .matchTitle("테스트 매칭방")
                .matchDescription("테스트용 매칭방입니다")
                .teamId(10L)
                .minAge(20)
                .maxAge(30)
                .genderCondition("ALL")
                .maxParticipants(4)
                .currentParticipants(1)
                .status("ACTIVE")
                .build();

        listRequest = MatchChatRoomListRequest.builder()
                .limit(10)
                .keyword("테스트")
                .build();

        listResponse = MatchChatRoomListResponse.builder()
                .rooms(Arrays.asList(createResponse))
                .count(1)
                .hasMore(false)
                .build();
    }

    @Test
    @DisplayName("매칭 채팅방 생성 성공")
    void createMatchChatRoom_Success() throws Exception {
        // given
        given(matchChatRoomService.createMatchChatRoom(any(MatchChatRoomCreateRequest.class)))
                .willReturn(createResponse);

        // when & then
        mockMvc.perform(post("/api/match-chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.matchId").value("match_1"))
                .andExpect(jsonPath("$.data.matchTitle").value("테스트 매칭방"))
                .andExpect(jsonPath("$.data.gameId").value(1))
                .andExpect(jsonPath("$.data.teamId").value(10))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(matchChatRoomService).createMatchChatRoom(any(MatchChatRoomCreateRequest.class));
    }

    @Test
    @DisplayName("매칭 채팅방 생성 실패 - 필수값 누락")
    void createMatchChatRoom_ValidationFailed() throws Exception {
        // given
        MatchChatRoomCreateRequest invalidRequest = MatchChatRoomCreateRequest.builder()
                .gameId(1L)
                // matchTitle 누락
                .teamId(10L)
                .minAge(20)
                .maxAge(30)
                .genderCondition("ALL")
                .maxParticipants(4)
                .build();

        // when & then
        mockMvc.perform(post("/api/match-chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("매칭 채팅방 생성 실패 - 서비스 예외")
    void createMatchChatRoom_ServiceException() throws Exception {
        // given
        given(matchChatRoomService.createMatchChatRoom(any(MatchChatRoomCreateRequest.class)))
                .willThrow(new ApiException(ErrorCode.SERVER_ERROR));

        // when & then
        mockMvc.perform(post("/api/match-chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isInternalServerError());

        verify(matchChatRoomService).createMatchChatRoom(any(MatchChatRoomCreateRequest.class));
    }

    @Test
    @DisplayName("매칭 채팅방 목록 조회 성공")
    void getMatchChatRoomList_Success() throws Exception {
        // given
        given(matchChatRoomService.getMatchChatRoomList(any(MatchChatRoomListRequest.class)))
                .willReturn(listResponse);

        // when & then
        mockMvc.perform(get("/api/match-chat-rooms")
                        .param("limit", "10")
                        .param("keyword", "테스트"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.rooms").isArray())
                .andExpect(jsonPath("$.data.rooms[0].matchId").value("match_1"))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.hasMore").value(false));

        verify(matchChatRoomService).getMatchChatRoomList(any(MatchChatRoomListRequest.class));
    }

    @Test
    @DisplayName("매칭 채팅방 목록 조회 - 빈 결과")
    void getMatchChatRoomList_EmptyResult() throws Exception {
        // given
        MatchChatRoomListResponse emptyResponse = MatchChatRoomListResponse.builder()
                .rooms(List.of())
                .count(0)
                .hasMore(false)
                .build();

        given(matchChatRoomService.getMatchChatRoomList(any(MatchChatRoomListRequest.class)))
                .willReturn(emptyResponse);

        // when & then
        mockMvc.perform(get("/api/match-chat-rooms")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.rooms").isEmpty())
                .andExpect(jsonPath("$.data.count").value(0))
                .andExpect(jsonPath("$.data.hasMore").value(false));
    }

    @Test
    @DisplayName("특정 매칭 채팅방 조회 성공")
    void getMatchChatRoom_Success() throws Exception {
        // given
        String matchId = "match_1";
        given(matchChatRoomService.getMatchChatRoom(matchId)).willReturn(createResponse);

        // when & then
        mockMvc.perform(get("/api/match-chat-rooms/{matchId}", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.matchId").value(matchId))
                .andExpect(jsonPath("$.data.matchTitle").value("테스트 매칭방"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(matchChatRoomService).getMatchChatRoom(matchId);
    }

    @Test
    @DisplayName("존재하지 않는 매칭 채팅방 조회")
    void getMatchChatRoom_NotFound() throws Exception {
        // given
        String matchId = "non_existent_match";
        given(matchChatRoomService.getMatchChatRoom(matchId)).willReturn(null);

        // when & then
        mockMvc.perform(get("/api/match-chat-rooms/{matchId}", matchId))
                .andExpect(status().isNotFound());

        verify(matchChatRoomService).getMatchChatRoom(matchId);
    }
    
    @Test
    @DisplayName("비활성 상태 매칭 채팅방 조회 시 예외 발생")
    void getMatchChatRoom_InactiveRoom_ThrowsException() throws Exception {
        // given
        String matchId = "inactive_match";
        given(matchChatRoomService.getMatchChatRoom(matchId))
                .willThrow(new ApiException(ErrorCode.MATCH_CHAT_ROOM_CLOSED));

        // when & then
        mockMvc.perform(get("/api/match-chat-rooms/{matchId}", matchId))
                .andExpect(status().isConflict());

        verify(matchChatRoomService).getMatchChatRoom(matchId);
    }

    @Test
    @DisplayName("매칭 채팅방 생성 - 나이 범위 검증 실패")
    void createMatchChatRoom_InvalidAgeRange() throws Exception {
        // given
        MatchChatRoomCreateRequest invalidRequest = MatchChatRoomCreateRequest.builder()
                .gameId(1L)
                .matchTitle("테스트 매칭방")
                .teamId(10L)
                .minAge(5) // 최소 나이 위반
                .maxAge(30)
                .genderCondition("ALL")
                .maxParticipants(4)
                .build();

        // when & then
        mockMvc.perform(post("/api/match-chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("매칭 채팅방 생성 - 최대 참여자 수 검증 실패")
    void createMatchChatRoom_InvalidMaxParticipants() throws Exception {
        // given
        MatchChatRoomCreateRequest invalidRequest = MatchChatRoomCreateRequest.builder()
                .gameId(1L)
                .matchTitle("테스트 매칭방")
                .teamId(10L)
                .minAge(20)
                .maxAge(30)
                .genderCondition("ALL")
                .maxParticipants(20) // 최대 참여자 수 위반
                .build();

        // when & then
        mockMvc.perform(post("/api/match-chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("매칭 채팅방 생성 - 제목 길이 검증 실패")
    void createMatchChatRoom_TitleTooLong() throws Exception {
        // given
        MatchChatRoomCreateRequest invalidRequest = MatchChatRoomCreateRequest.builder()
                .gameId(1L)
                .matchTitle("이것은 매우 긴 제목입니다 이것은 매우 긴 제목입니다") // 20자 초과
                .teamId(10L)
                .minAge(20)
                .maxAge(30)
                .genderCondition("ALL")
                .maxParticipants(4)
                .build();

        // when & then
        mockMvc.perform(post("/api/match-chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
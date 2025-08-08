package com.ssafy.chat.match.controller;

import com.ssafy.chat.global.constants.ErrorCode;
import com.ssafy.chat.global.constants.SuccessCode;
import com.ssafy.chat.global.exception.ApiException;
import com.ssafy.chat.global.response.ApiResponse;
import com.ssafy.chat.match.dto.*;
import com.ssafy.chat.match.service.MatchChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * 매칭 채팅방 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/match-chat-rooms")
@RequiredArgsConstructor
public class MatchChatRoomController {

    private final MatchChatRoomService matchChatRoomService;

    /**
     * 매칭 채팅방 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MatchChatRoomCreateResponse>> createMatchChatRoom(
            @Valid @RequestBody MatchChatRoomCreateRequest request) {
        
        MatchChatRoomCreateResponse response = matchChatRoomService.createMatchChatRoom(request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_CREATED, response));
    }

    /**
     * 매칭 채팅방 목록 조회 (무한 스크롤)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MatchChatRoomListResponse>> getMatchChatRoomList(
            @Valid MatchChatRoomListRequest request) {
        
        MatchChatRoomListResponse response = matchChatRoomService.getMatchChatRoomList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 매칭 채팅방 조회
     */
    @GetMapping("/{matchId}")
    public ResponseEntity<ApiResponse<MatchChatRoomCreateResponse>> getMatchChatRoom(@PathVariable String matchId) {
        
        MatchChatRoomCreateResponse response = matchChatRoomService.getMatchChatRoom(matchId);
        if (response == null) {
            throw new ApiException(ErrorCode.MATCH_CHAT_ROOM_NOT_FOUND);
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
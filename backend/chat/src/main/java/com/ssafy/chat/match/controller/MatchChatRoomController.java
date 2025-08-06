package com.ssafy.chat.match.controller;

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
    public ResponseEntity<MatchChatRoomResponse> createMatchChatRoom(
            @Valid @RequestBody MatchChatRoomCreateRequest request) {
        
        MatchChatRoomResponse response = matchChatRoomService.createMatchChatRoom(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 매칭 채팅방 목록 조회 (무한 스크롤)
     */
    @GetMapping
    public ResponseEntity<MatchChatRoomListResponse> getMatchChatRoomList(
            @Valid MatchChatRoomListRequest request) {
        
        MatchChatRoomListResponse response = matchChatRoomService.getMatchChatRoomList(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 매칭 채팅방 조회
     */
    @GetMapping("/{matchId}")
    public ResponseEntity<MatchChatRoomResponse> getMatchChatRoom(@PathVariable String matchId) {
        
        MatchChatRoomResponse response = matchChatRoomService.getMatchChatRoom(matchId);
        return ResponseEntity.ok(response);
    }
}
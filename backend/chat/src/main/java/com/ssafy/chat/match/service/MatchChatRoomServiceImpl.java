package com.ssafy.chat.match.service;

import com.ssafy.chat.common.dto.AuthResult;
import com.ssafy.chat.common.dto.UserInfo;
import com.ssafy.chat.common.util.KSTTimeUtil;
import com.ssafy.chat.match.dto.MatchChatRoom;
import com.ssafy.chat.match.dto.MatchChatRoomCreateRequest;
import com.ssafy.chat.match.dto.MatchChatRoomCreateResponse;
import com.ssafy.chat.match.dto.MatchChatRoomListRequest;
import com.ssafy.chat.match.dto.MatchChatRoomListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 매칭 채팅방 서비스 구현체 (리팩토링 버전)
 * 각 책임을 별도 서비스로 위임하는 퍼사드 패턴 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchChatRoomServiceImpl implements MatchChatRoomService {
    
    private final MatchChatRoomAuthService authService;
    private final MatchChatRoomCreationService creationService;
    private final MatchChatRoomQueryService queryService;
    private final MatchChatRoomStorageService storageService;
    
    @Override
    public MatchChatRoomCreateResponse createMatchChatRoom(MatchChatRoomCreateRequest request, String jwtToken) {
        log.info("매칭 채팅방 생성 요청 - gameId: {}, title: {}", request.getGameId(), request.getMatchTitle());
        
        // 1. 인증 및 권한 확인
        AuthResult authResult = authService.authenticateForCreation(jwtToken, request);
        
        // 2. 채팅방 생성 - 임시로 JWT 토큰 사용
        MatchChatRoomCreateResponse createResponse = creationService.createMatchChatRoom(request, jwtToken);
        
        // 응답에서 MatchChatRoom 객체 생성 (임시)
        MatchChatRoom chatRoom = MatchChatRoom.builder()
            .matchId(createResponse.getMatchId())
            .gameId(createResponse.getGameId())
            .matchTitle(createResponse.getMatchTitle())
            .matchDescription(createResponse.getMatchDescription())
            .teamId(createResponse.getTeamId())
            .minAge(createResponse.getMinAge())
            .maxAge(createResponse.getMaxAge())
            .genderCondition(createResponse.getGenderCondition())
            .maxParticipants(createResponse.getMaxParticipants())
            .currentParticipants(createResponse.getCurrentParticipants())
            .minWinRate(createResponse.getMinWinRate())
            .ownerId(authResult.getUserInfo().getUserId().toString())
            .creatorNickname(createResponse.getCreatorNickname())
            .createdAt(createResponse.getCreatedAt())
            .lastActivityAt(KSTTimeUtil.nowAsString())
            .status(createResponse.getStatus())
            .build();
        
        // 3. 저장
        storageService.saveMatchChatRoom(chatRoom, extractGameDate(authResult));
        
        // 4. 응답 생성
        return buildCreateResponse(chatRoom);
    }
    
    @Override
    public MatchChatRoomListResponse getMatchChatRoomList(MatchChatRoomListRequest request) {
        log.debug("매칭 채팅방 목록 조회 - keyword: {}, limit: {}", request.getKeyword(), request.getLimit());
        
        return queryService.getMatchChatRoomList(request);
    }
    
    @Override
    public MatchChatRoomCreateResponse getMatchChatRoom(String matchId) {
        log.debug("매칭 채팅방 조회 - matchId: {}", matchId);
        
        MatchChatRoom chatRoom = queryService.getMatchChatRoomDetail(matchId);
        return chatRoom != null ? buildCreateResponse(chatRoom) : null;
    }
    
    /**
     * 게임 날짜 추출
     */
    private String extractGameDate(AuthResult authResult) {
        // AuthResult의 추가정보에서 게임 날짜를 추출하거나 현재 날짜 사용
        if (authResult.getAdditionalInfo() != null) {
            Object gameDate = authResult.getAdditionalInfo().get("gameDate");
            if (gameDate instanceof String) {
                return (String) gameDate;
            }
        }
        return KSTTimeUtil.todayAsString();
    }
    
    /**
     * 생성 응답 구성
     */
    private MatchChatRoomCreateResponse buildCreateResponse(MatchChatRoom chatRoom) {
        return MatchChatRoomCreateResponse.builder()
                .matchId(chatRoom.getMatchId())
                .gameId(chatRoom.getGameId())
                .matchTitle(chatRoom.getMatchTitle())
                .matchDescription(chatRoom.getMatchDescription())
                .teamId(chatRoom.getTeamId())
                .minAge(chatRoom.getMinAge())
                .maxAge(chatRoom.getMaxAge())
                .genderCondition(chatRoom.getGenderCondition())
                .maxParticipants(chatRoom.getMaxParticipants())
                .currentParticipants(chatRoom.getCurrentParticipants())
                .minWinRate(chatRoom.getMinWinRate())
                .createdAt(chatRoom.getCreatedAt())
                .creatorNickname(chatRoom.getCreatorNickname())
                .build();
    }
}
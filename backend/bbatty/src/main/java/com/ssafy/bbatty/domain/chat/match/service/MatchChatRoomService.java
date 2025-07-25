package com.ssafy.bbatty.domain.chat.match.service;

import com.ssafy.bbatty.domain.chat.match.dto.MatchChatRoomCreateRequest;
import com.ssafy.bbatty.domain.chat.match.dto.MatchChatRoomInfo;
import com.ssafy.bbatty.domain.chat.match.dto.MatchChatRoomSearchCriteria;

import java.util.List;

/**
 * 매칭 채팅방 관리 서비스
 */
public interface MatchChatRoomService {

    /**
     * 매칭 채팅방 생성
     * @param request 채팅방 생성 요청
     * @param creatorId 방장 ID
     * @return 생성된 채팅방 ID
     */
    String createMatchChatRoom(MatchChatRoomCreateRequest request, Long creatorId);

    /**
     * 방장 조건 검증
     * @param userId 사용자 ID
     * @param request 생성 요청 (조건 포함)
     * @return 조건 부합 여부
     */
    boolean validateCreatorConditions(Long userId, MatchChatRoomCreateRequest request);

    /**
     * 매칭 채팅방 참여
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 참여 성공 여부
     */
    boolean joinMatchChatRoom(String roomId, Long userId);

    /**
     * 매칭 채팅방 나가기
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    void leaveMatchChatRoom(String roomId, Long userId);

    /**
     * 참여자 추방 (방장 권한)
     * @param roomId 채팅방 ID
     * @param ownerId 방장 ID
     * @param targetUserId 추방 대상 ID
     * @return 추방 성공 여부
     */
    boolean kickParticipant(String roomId, Long ownerId, Long targetUserId);

    /**
     * 입장 조건 수정 (방장 권한)
     * @param roomId 채팅방 ID
     * @param ownerId 방장 ID
     * @param newConditions 새로운 조건
     * @return 수정 성공 여부
     */
    boolean updateRoomConditions(String roomId, Long ownerId, MatchChatRoomCreateRequest newConditions);

    /**
     * 매칭 채팅방 목록 조회 (검색/필터링)
     * @param criteria 검색 조건
     * @return 조건에 맞는 채팅방 목록
     */
    List<MatchChatRoomInfo> searchMatchChatRooms(MatchChatRoomSearchCriteria criteria);

    /**
     * 사용자 참여 중인 채팅방 목록
     * @param userId 사용자 ID
     * @return 참여 중인 채팅방 목록
     */
    List<MatchChatRoomInfo> getUserMatchChatRooms(Long userId);

    /**
     * 경기 날짜 지난 채팅방 자동 삭제
     * @return 삭제된 채팅방 수
     */
    int cleanupExpiredMatchRooms();

    /**
     * 참여자 없는 빈 방 자동 삭제
     * @return 삭제된 채팅방 수
     */
    int cleanupEmptyMatchRooms();

    /**
     * 승리 요정 여부 확인
     * @param userId 사용자 ID
     * @return 승리 요정 여부 (시즌 승률 70% 이상 + 10경기 이상)
     */
    boolean checkWinningFairyStatus(Long userId);
}
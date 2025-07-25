package com.ssafy.bbatty.domain.chat.game.service;

import com.ssafy.bbatty.domain.chat.game.dto.TeamChatRoomInfo;

import java.util.List;

/**
 * 경기 직관 채팅방 관리
 * - 하나의 경기당 2개 팀별 채팅방 생성
 * - KBO 10개 팀 고정
 */
public interface GameChatRoomService {
    /**
     * 경기 팀별 채팅방 생성
     * @param gameId 경기 ID
     * @return 생성된 2개 채팅방 정보 (홈팀, 어웨이팀)
     */
    List<TeamChatRoomInfo> createTeamChatRooms(Long gameId);

    /**
     * 경기 시작 2시간 전 자동 활성화
     * @parma gameID 경기 Id
     */
    void activateGameChatRooms(Long gameId);

    /**
     * 자정 12시 자동 비활성화
     * @param gameID 경기 ID
     */
    void deactivateGameChatRooms(Long gameId);

    /**
     * 사용자 응원팀 기반 입장 가능 채팅방 조회
     * @param userId 사용자 Id (웅원방 정보 포함)
     * @return 입장 가능한 채팅방 목록
     */
    TeamChatRoomInfo getAvailableTeamChatRooms(Long userId);

    /**
     * 오늘 경기 채팅방 목록 (팀별) - 디버깅용
     * @return 오늘 경기의 모든 팀 채팅방
     */
    List<TeamChatRoomInfo> getTodayTeamChatRooms();

    /** 사용자 직관 인증 확인
     * @param userId 사용자 ID
     * @param gameId 경기 ID
     * @return 직관 인증 여부
     */
    boolean isUserAuthenticated(Long userId, Long gameId);
    /**
     * 승리요정 여부 확인
     * @param userId 사용자 ID
     * @return 승리요정 여부
     */
    boolean isWinningFairy(Long userId);
    /**
     * 더블헤더 채팅방 통합처리
     * @param gameId1 첫 번째 경기 Id
     * @param gameId2 두 번째 경기 Id
     */
    void mergeDoubleHeaderChatRooms(Long gameId1, Long gameId2);
    /**
     * 트래픽 급증 시 챗봇 공지 전송
     * @param gameId 경기 Id
     * @param teamId 팀 Id
     */
    void sendTrafficAlert(Long gameId, Long teamId);











}

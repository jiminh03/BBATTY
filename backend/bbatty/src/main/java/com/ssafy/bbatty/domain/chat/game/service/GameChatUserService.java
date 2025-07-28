package com.ssafy.bbatty.domain.chat.game.service;

import java.util.Set;

/**
 * 게임 채팅 사용자 관리 서비스 인터페이스
 */
public interface GameChatUserService {

    /**
     * 사용자를 특정 팀 채팅방에 추가
     * @param teamId 팀 ID
     * @param userId 사용자 ID
     * @param userName 사용자 이름
     */
    void addUser(String teamId, String userId, String userName);

    /**
     * 특정 팀 채팅방에서 사용자 제거
     * @param teamId 팀 ID
     * @param userId 사용자 ID
     */
    void removeUser(String teamId, String userId);

    /**
     * 특정 팀 채팅방에서 사용자의 활동 시간 업데이트
     * @param teamId 팀 ID
     * @param userId 사용자 ID
     * @param userName 사용자 이름
     */
    void updateUserActivity(String teamId, String userId, String userName);

    /**
     * 특정 팀 채팅방의 현재 접속자 수 조회
     * @param teamId 팀 ID
     * @return 접속자 수
     */
    long getConnectedUserCount(String teamId);

    /**
     * 특정 팀 채팅방의 접속자 목록 조회
     * @param teamId 팀 ID
     * @return 접속자 ID 집합
     */
    Set<Object> getConnectedUsers(String teamId);

    /**
     * 특정 사용자의 밴 상태 확인 (추후 구현)
     * @param teamId 팀 ID
     * @param userId 사용자 ID
     * @return 밴 여부
     */
    boolean isUserBanned(String teamId, String userId);

    /**
     * 특정 사용자가 해당 팀 멤버인지 여부 확인 (추후 구현)
     * @param teamId 팀 ID
     * @param userId 사용자 ID
     * @return 멤버 여부
     */
    boolean isTeamMember(String teamId, String userId);

}

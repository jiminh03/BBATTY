package com.ssafy.bbatty.domain.chat.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 채팅 관련 Redis 키 생성 유틸리티
 */
@Component
public class ChatRedisKeyGenerator {

    @Value("${chat.redis.key-prefix:chat:}")
    private String keyPrefix;

    // ========== 경기 채팅방 (팀별) ==========

    /**
     * 팀별 채팅방 기본 키
     * @param gameId 경기 ID
     * @param teamId 팀 ID
     * @return 예: "chat:game:123:team:1"
     */
    public String createTeamChatRoomKey(Long gameId, Long teamId) {
        return keyPrefix + "game:" + gameId + ":team:" + teamId;
    }

    /**
     * 팀별 채팅방 상태 키
     */
    public String createTeamChatRoomStatusKey(Long gameId, Long teamId) {
        return createTeamChatRoomKey(gameId, teamId) + ":status";
    }

    /**
     * 팀별 채팅방 사용자 목록 키
     */
    public String createTeamChatRoomUsersKey(Long gameId, Long teamId) {
        return createTeamChatRoomKey(gameId, teamId) + ":users";
    }

    /**
     * 팀별 채팅방 트래픽 키
     */
    public String createTeamChatRoomTrafficKey(Long gameId, Long teamId) {
        return createTeamChatRoomKey(gameId, teamId) + ":traffic";
    }

    /**
     * 팀별 채팅방 Pub/Sub 채널
     */
    public String createTeamChatRoomChannelKey(Long gameId, Long teamId) {
        return "chat:game_" + gameId + "_team_" + teamId;
    }

    // ========== 매칭 채팅방 ==========

    /**
     * 매칭 채팅방 기본 키
     * @param matchRoomId 매칭 채팅방 ID
     * @return 예: "chat:match:room123456"
     */
    public String createMatchChatRoomKey(String matchRoomId) {
        return keyPrefix + "match:" + matchRoomId;
    }

    /**
     * 매칭 채팅방 상태 키
     */
    public String createMatchChatRoomStatusKey(String matchRoomId) {
        return createMatchChatRoomKey(matchRoomId) + ":status";
    }

    /**
     * 매칭 채팅방 참여자 목록 키
     */
    public String createMatchChatRoomParticipantsKey(String matchRoomId) {
        return createMatchChatRoomKey(matchRoomId) + ":participants";
    }

    /**
     * 매칭 채팅방 조건 키
     */
    public String createMatchChatRoomConditionsKey(String matchRoomId) {
        return createMatchChatRoomKey(matchRoomId) + ":conditions";
    }

    /**
     * 매칭 채팅방 Pub/Sub 채널
     */
    public String createMatchChatRoomChannelKey(String matchRoomId) {
        return "chat:" + matchRoomId;
    }

    // ========== 사용자 관련 ==========

    /**
     * 사용자 직관 인증 키
     */
    public String createUserAuthKey(Long userId, Long gameId) {
        return keyPrefix + "auth:user:" + userId + ":game:" + gameId;
    }

    /**
     * 사용자 승리요정 상태 키
     */
    public String createUserWinningFairyKey(Long userId) {
        return keyPrefix + "user:" + userId + ":winning_fairy";
    }

    /**
     * 매칭 채팅방 목록 인덱스 키 (검색용)
     */
    public String createMatchRoomIndexKey(String indexType) {
        return keyPrefix + "match:index:" + indexType;
    }
}
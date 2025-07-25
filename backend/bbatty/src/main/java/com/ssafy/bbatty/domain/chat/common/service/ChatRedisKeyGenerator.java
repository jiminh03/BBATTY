package com.ssafy.bbatty.domain.chat.common.service;

import com.ssafy.bbatty.domain.chat.common.enums.ChatRoomType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 채팅 관련 Redis 키 생성 유틸리티
 * 일관된 키 네이밍 규칙을 제공하고 타입별 키 패턴을 관리
 */
@Component
public class ChatRedisKeyGenerator {

    @Value("${chat.redis.key-prefix:chat:}")
    private String keyPrefix;

    // 기본 키 패턴들
    private static final String STATUS_SUFFIX = ":status";
    private static final String META_SUFFIX = ":meta";
    private static final String USERS_SUFFIX = ":users";
    private static final String ACTIVITY_SUFFIX = ":activity";
    private static final String CHANNEL_SUFFIX = ":channel";
    private static final String TRAFFIC_CURRENT_SUFFIX = ":traffic:current";
    private static final String TRAFFIC_HISTORY_SUFFIX = ":traffic:history";
    private static final String MESSAGES_SUFFIX = ":messages";
    private static final String SEQUENCE_SUFFIX = ":sequence";
    private static final String LOCK_SUFFIX = ":lock";

    /**
     * 기본 채팅방 키 생성
     * @param roomType 방 타입
     * @param roomId 방 ID
     * @return 기본 키 (예: "chat:game:tigers")
     */
    public String createRoomKey(ChatRoomType roomType, String roomId) {
        return keyPrefix + roomType.getCode() + ":" + roomId;
    }

    /**
     * 채팅방 상태 키 생성
     * @return 예: "chat:game:tigers:status"
     */
    public String createStatusKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + STATUS_SUFFIX;
    }

    /**
     * 채팅방 메타데이터 키 생성
     * @return 예: "chat:game:tigers:meta"
     */
    public String createMetaKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + META_SUFFIX;
    }

    /**
     * 사용자 목록 키 생성
     * @return 예: "chat:game:tigers:users"
     */
    public String createUsersKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + USERS_SUFFIX;
    }

    /**
     * 사용자 활동 키 생성
     * @return 예: "chat:game:tigers:activity"
     */
    public String createActivityKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + ACTIVITY_SUFFIX;
    }

    /**
     * Pub/Sub 채널 키 생성
     * @return 예: "chat:game:tigers:channel"
     */
    public String createChannelKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + CHANNEL_SUFFIX;
    }

    /**
     * 현재 트래픽 키 생성
     * @return 예: "chat:game:tigers:traffic:current"
     */
    public String createTrafficCurrentKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + TRAFFIC_CURRENT_SUFFIX;
    }

    /**
     * 트래픽 히스토리 키 생성
     * @return 예: "chat:game:tigers:traffic:history"
     */
    public String createTrafficHistoryKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + TRAFFIC_HISTORY_SUFFIX;
    }

    /**
     * 메시지 저장 키 생성
     * @return 예: "chat:game:tigers:messages"
     */
    public String createMessagesKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + MESSAGES_SUFFIX;
    }

    /**
     * 메시지 시퀀스 키 생성
     * @return 예: "chat:game:tigers:sequence"
     */
    public String createSequenceKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + SEQUENCE_SUFFIX;
    }

    /**
     * 분산 락 키 생성
     * @return 예: "chat:game:tigers:lock"
     */
    public String createLockKey(ChatRoomType roomType, String roomId) {
        return createRoomKey(roomType, roomId) + LOCK_SUFFIX;
    }

    // ========== 편의 메서드들 (기존 호환성 유지) ==========

    /**
     * 게임 채팅 전용 키들
     */
    public String createGameStatusKey(String teamId) {
        return createStatusKey(ChatRoomType.GAME, teamId);
    }

    public String createGameMetaKey(String teamId) {
        return createMetaKey(ChatRoomType.GAME, teamId);
    }

    public String createGameUsersKey(String teamId) {
        return createUsersKey(ChatRoomType.GAME, teamId);
    }

    public String createGameActivityKey(String teamId) {
        return createActivityKey(ChatRoomType.GAME, teamId);
    }

    public String createGameChannelKey(String teamId) {
        return createChannelKey(ChatRoomType.GAME, teamId);
    }

    public String createGameTrafficCurrentKey(String teamId) {
        return createTrafficCurrentKey(ChatRoomType.GAME, teamId);
    }

    public String createGameTrafficHistoryKey(String teamId) {
        return createTrafficHistoryKey(ChatRoomType.GAME, teamId);
    }

    /**
     * 매칭 채팅 전용 키들
     */
    public String createMatchStatusKey(String matchId) {
        return createStatusKey(ChatRoomType.MATCH, matchId);
    }

    public String createMatchMetaKey(String matchId) {
        return createMetaKey(ChatRoomType.MATCH, matchId);
    }

    public String createMatchUsersKey(String matchId) {
        return createUsersKey(ChatRoomType.MATCH, matchId);
    }

    public String createMatchActivityKey(String matchId) {
        return createActivityKey(ChatRoomType.MATCH, matchId);
    }

    public String createMatchChannelKey(String matchId) {
        return createChannelKey(ChatRoomType.MATCH, matchId);
    }

    /**
     * Pub/Sub 채널명 생성 (기존 RedisPubSubService 호환)
     * @param roomId 방 ID
     * @return 채널명 (예: "chat:tigers")
     */
    public String createPubSubChannelName(String roomId) {
        return keyPrefix.substring(0, keyPrefix.length() - 1) + ":" + roomId;
    }

    /**
     * roomId에서 방 타입 추출 (키 패턴 기반 추정)
     * @param roomId 방 ID
     * @return 추정된 방 타입
     */
    public ChatRoomType inferRoomType(String roomId) {
        if (roomId == null) {
            return null;
        }

        // 매칭 관련 패턴
        if (roomId.startsWith("match_") || roomId.contains("match")) {
            return ChatRoomType.MATCH;
        }

        // 기본적으로 게임 채팅으로 간주
        return ChatRoomType.GAME;
    }

    /**
     * 키에서 roomId 추출
     * @param redisKey Redis 키
     * @return roomId
     */
    public String extractRoomId(String redisKey) {
        if (redisKey == null || !redisKey.startsWith(keyPrefix)) {
            return null;
        }

        String withoutPrefix = redisKey.substring(keyPrefix.length());

        // "game:tigers:status" → "tigers" 추출
        String[] parts = withoutPrefix.split(":");
        if (parts.length >= 2) {
            return parts[1];
        }

        return null;
    }
}
package com.ssafy.chat.global.constants;

/**
 * 채팅 시스템 Redis 키 전략 - 도메인 주도 설계
 * 
 * 📋 키 구조: {domain}:{aggregate}:{entity_id}:{attribute}
 * 📋 TTL 전략: 도메인 특성에 맞게 설정
 * 📋 네이밍 규칙: snake_case 사용으로 가독성 향상
 */
public class ChatRedisKey {

    // ===========================================
    // CHAT ROOM 도메인 - 채팅방 관련
    // ===========================================

    /** 채팅방별 사용자 목록: chat:room:users:{roomId} (Hash) */
    public static final String CHAT_ROOM_USERS = "chat:room:users:";
    
    /** 채팅방 정보: chat:room:info:{roomId} (Hash) */
    public static final String CHAT_ROOM_INFO = "chat:room:info:";
    
    /** 세션별 채팅방 매핑: chat:session:room:{sessionId} (String) */
    public static final String CHAT_SESSION_ROOM_MAPPING = "chat:session:room:";

    // ===========================================
    // WATCH CHAT 도메인 - 관전 채팅 관련
    // ===========================================

    /** 관전 채팅방 정보: watch:room:info:{roomId} (Hash) */
    public static final String WATCH_ROOM_INFO = "watch:room:info:";
    
    /** 관전 채팅 트래픽 모니터링: watch:traffic:{roomId}:{minute} (String) */
    public static final String WATCH_TRAFFIC_COUNT = "watch:traffic:";
    
    /** 관전 채팅 Redis Pub/Sub 채널: watch:channel:{roomId} */
    public static final String WATCH_PUBSUB_CHANNEL = "watch:channel:";

    // ===========================================
    // MATCH CHAT 도메인 - 매칭 채팅 관련
    // ===========================================

    /** 매칭 채팅방 정보: match:room:info:{matchId} (Hash) */
    public static final String MATCH_ROOM_INFO = "match:room:info:";
    
    /** 매칭 채팅방 목록: match:room:list (Set) */
    public static final String MATCH_ROOM_LIST = "match:room:list";
    
    /** 날짜별 매칭 채팅방 목록: match:room:list:date:{date} (Set) */
    public static final String MATCH_ROOM_LIST_BY_DATE = "match:room:list:date:";


    // ===========================================
    // AUTH & SESSION 도메인 - 인증 및 세션 관련
    // ===========================================

    /** 사용자 세션 정보: chat:user:session:{sessionToken} (Hash) */
    public static final String CHAT_USER_SESSION = "chat:user:session:";
    
    /** 채팅 인증 결과: chat:auth:result:{requestId} (Hash) */
    public static final String CHAT_AUTH_RESULT = "chat:auth:result:";

    // ===========================================
    // 유틸리티 메서드
    // ===========================================

    /**
     * 채팅방 사용자 목록 키 생성
     */
    public static String getChatRoomUsersKey(String roomId) {
        return CHAT_ROOM_USERS + roomId;
    }

    /**
     * 채팅방 정보 키 생성
     */
    public static String getChatRoomInfoKey(String roomId) {
        return CHAT_ROOM_INFO + roomId;
    }

    /**
     * 세션-채팅방 매핑 키 생성
     */
    public static String getSessionRoomMappingKey(String sessionId) {
        return CHAT_SESSION_ROOM_MAPPING + sessionId;
    }

    /**
     * 관전 채팅방 정보 키 생성
     */
    public static String getWatchRoomInfoKey(String roomId) {
        return WATCH_ROOM_INFO + roomId;
    }

    /**
     * 관전 채팅 트래픽 키 생성
     */
    public static String getWatchTrafficKey(String roomId, String minute) {
        return WATCH_TRAFFIC_COUNT + roomId + ":" + minute;
    }

    /**
     * 관전 채팅 Pub/Sub 채널명 생성
     */
    public static String getWatchPubSubChannel(String roomId) {
        return WATCH_PUBSUB_CHANNEL + roomId;
    }

    /**
     * 매칭 채팅방 정보 키 생성
     */
    public static String getMatchRoomInfoKey(String matchId) {
        return MATCH_ROOM_INFO + matchId;
    }
    
    /**
     * 날짜별 매칭 채팅방 목록 키 생성
     */
    public static String getMatchRoomListByDateKey(String date) {
        return MATCH_ROOM_LIST_BY_DATE + date;
    }


    /**
     * 사용자 세션 정보 키 생성
     */
    public static String getUserSessionKey(String sessionToken) {
        return CHAT_USER_SESSION + sessionToken;
    }

    /**
     * 채팅 인증 결과 키 생성
     */
    public static String getChatAuthResultKey(String requestId) {
        return CHAT_AUTH_RESULT + requestId;
    }
}
//package com.ssafy.bbatty.domain.chat.match.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.Set;
//import java.util.concurrent.TimeUnit;
//
///**
// * 매칭 채팅 세션 관리 서비스
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MatchChatSessionService {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    private static final String SESSION_KEY_PREFIX = "match_chat:sessions:";
//    private static final String ROOM_KEY_PREFIX = "match_chat:rooms:";
//    private static final long SESSION_TTL = 24; // 24시간
//
//    /**
//     * 매칭 세션에 사용자 추가
//     */
//    public void addUserToSession(String sessionId, String userId, String userName) {
//        try {
//            String key = SESSION_KEY_PREFIX + sessionId;
//            redisTemplate.opsForHash().put(key, userId, userName);
//            redisTemplate.expire(key, SESSION_TTL, TimeUnit.HOURS);
//
//            log.debug("매칭 세션 사용자 추가 - sessionId: {}, userId: {}", sessionId, userId);
//        } catch (Exception e) {
//            log.error("매칭 세션 사용자 추가 실패 - sessionId: {}, userId: {}", sessionId, userId, e);
//        }
//    }
//
//    /**
//     * 매칭 세션에서 사용자 제거
//     */
//    public void removeUserFromSession(String sessionId, String userId) {
//        try {
//            String key = SESSION_KEY_PREFIX + sessionId;
//            redisTemplate.opsForHash().delete(key, userId);
//
//            log.debug("매칭 세션 사용자 제거 - sessionId: {}, userId: {}", sessionId, userId);
//        } catch (Exception e) {
//            log.error("매칭 세션 사용자 제거 실패 - sessionId: {}, userId: {}", sessionId, userId, e);
//        }
//    }
//
//    /**
//     * 세션 접속자 수 조회
//     */
//    public long getSessionUserCount(String sessionId) {
//        try {
//            String key = SESSION_KEY_PREFIX + sessionId;
//            return redisTemplate.opsForHash().size(key);
//        } catch (Exception e) {
//            log.error("세션 접속자 수 조회 실패 - sessionId: {}", sessionId, e);
//            return 0;
//        }
//    }
//
//    /**
//     * 세션 접속자 목록 조회
//     */
//    public Set<Object> getSessionUsers(String sessionId) {
//        try {
//            String key = SESSION_KEY_PREFIX + sessionId;
//            return redisTemplate.opsForHash().keys(key);
//        } catch (Exception e) {
//            log.error("세션 접속자 목록 조회 실패 - sessionId: {}", sessionId, e);
//            return Set.of();
//        }
//    }
//
//    /**
//     * 매칭 방 생성
//     */
//    public void createMatchRoom(String roomId, String matchType) {
//        try {
//            String key = ROOM_KEY_PREFIX + roomId;
//            redisTemplate.opsForHash().put(key, "matchType", matchType);
//            redisTemplate.opsForHash().put(key, "createdAt", System.currentTimeMillis());
//            redisTemplate.opsForHash().put(key, "status", "waiting");
//            redisTemplate.expire(key, SESSION_TTL, TimeUnit.HOURS);
//
//            log.debug("매칭 방 생성 - roomId: {}, matchType: {}", roomId, matchType);
//        } catch (Exception e) {
//            log.error("매칭 방 생성 실패 - roomId: {}", roomId, e);
//        }
//    }
//
//    /**
//     * 매칭 방 상태 업데이트
//     */
//    public void updateMatchRoomStatus(String roomId, String status) {
//        try {
//            String key = ROOM_KEY_PREFIX + roomId;
//            redisTemplate.opsForHash().put(key, "status", status);
//            redisTemplate.opsForHash().put(key, "updatedAt", System.currentTimeMillis());
//
//            log.debug("매칭 방 상태 업데이트 - roomId: {}, status: {}", roomId, status);
//        } catch (Exception e) {
//            log.error("매칭 방 상태 업데이트 실패 - roomId: {}", roomId, e);
//        }
//    }
//
//    /**
//     * 매칭 방 삭제
//     */
//    public void deleteMatchRoom(String roomId) {
//        try {
//            String key = ROOM_KEY_PREFIX + roomId;
//            redisTemplate.delete(key);
//
//            log.debug("매칭 방 삭제 - roomId: {}", roomId);
//        } catch (Exception e) {
//            log.error("매칭 방 삭제 실패 - roomId: {}", roomId, e);
//        }
//    }
//
//}
//package com.ssafy.bbatty.domain.chat.match.service.impl;
//
//import com.ssafy.bbatty.domain.chat.match.service.MatchChatRoomService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.concurrent.TimeUnit;
//
///**
// * 매칭 채팅방 관리 서비스 구현체
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MatchChatRoomServiceImpl implements MatchChatRoomService {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    private static final String MATCH_ROOM_PREFIX = "match_room:";
//    private static final String ACTIVE_MATCHES_KEY = "active_matches";
//
//    @Override
//    public boolean isMatchRoomActive(String matchId) {
//        try {
//            String key = MATCH_ROOM_PREFIX + matchId;
//            Object active = redisTemplate.opsForHash().get(key, "active");
//            return Boolean.TRUE.equals(active);
//        } catch (Exception e) {
//            log.error("매칭방 활성화 상태 확인 실패 - matchId: {}", matchId, e);
//            return false;
//        }
//    }
//
//    @Override
//    public Long getParticipantCount(String matchId) {
//        try {
//            String key = MATCH_ROOM_PREFIX + matchId;
//            Object count = redisTemplate.opsForHash().get(key, "participantCount");
//            return count != null ? Long.valueOf(count.toString()) : 0L;
//        } catch (Exception e) {
//            log.error("참가자 수 조회 실패 - matchId: {}", matchId, e);
//            return 0L;
//        }
//    }
//
//    @Override
//    public Integer getMaxParticipants(String matchId) {
//        try {
//            String key = MATCH_ROOM_PREFIX + matchId;
//            Object max = redisTemplate.opsForHash().get(key, "maxParticipants");
//            return max != null ? Integer.valueOf(max.toString()) : 10;
//        } catch (Exception e) {
//            log.error("최대 참가자 수 조회 실패 - matchId: {}", matchId, e);
//            return 10; // 기본값
//        }
//    }
//
//    @Override
//    public boolean isUserAlreadyInMatch(String matchId, String userId) {
//        try {
//            String key = "match_participants:" + matchId;
//            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, userId));
//        } catch (Exception e) {
//            log.error("사용자 참여 확인 실패 - matchId: {}, userId: {}", matchId, userId, e);
//            return false;
//        }
//    }
//
//    @Override
//    public Integer getRequiredParticipants(String matchId) {
//        try {
//            String key = MATCH_ROOM_PREFIX + matchId;
//            Object required = redisTemplate.opsForHash().get(key, "requiredParticipants");
//            return required != null ? Integer.valueOf(required.toString()) : 2;
//        } catch (Exception e) {
//            log.error("필요 참가자 수 조회 실패 - matchId: {}", matchId, e);
//            return 2; // 기본값
//        }
//    }
//
//    @Override
//    public void cleanupMatchRoom(String matchId) {
//        try {
//            String roomKey = MATCH_ROOM_PREFIX + matchId;
//            String participantsKey = "match_participants:" + matchId;
//
//            redisTemplate.delete(roomKey);
//            redisTemplate.delete(participantsKey);
//            redisTemplate.opsForSet().remove(ACTIVE_MATCHES_KEY, matchId);
//
//            log.info("매칭방 정리 완료 - matchId: {}", matchId);
//        } catch (Exception e) {
//            log.error("매칭방 정리 실패 - matchId: {}", matchId, e);
//        }
//    }
//
//    @Override
//    public void cleanupExpiredRooms(LocalDateTime expireTime) {
//        try {
//            // 만료된 방들 정리 로직
//            log.info("만료된 매칭방 정리 실행 - expireTime: {}", expireTime);
//        } catch (Exception e) {
//            log.error("만료된 방 정리 실패", e);
//        }
//    }
//}

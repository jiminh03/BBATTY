package com.ssafy.bbatty.domain.chat.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 게임 채팅 사용자 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameChatUserServiceImpl implements GameChatUserService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_KEY_PREFIX = "game_chat:users:";
    private static final String USER_ACTIVITY_PREFIX = "game_chat:activity:";
    private static final long USER_TTL = 24; // 24시간

    @Override
    public void addUser(String teamId, String userId, String userName) {
        try {
            String key = USER_KEY_PREFIX + teamId;
            redisTemplate.opsForHash().put(key, userId, userName);
            redisTemplate.expire(key, USER_TTL, TimeUnit.HOURS);

            // 사용자 활동 시간 업데이트
            updateUserActivity(teamId, userId, userName);

            log.debug("사용자 추가 완료 - teamId: {}, userId: {}", teamId, userId);
        } catch (Exception e) {
            log.error("사용자 추가 실패 - teamId: {}, userId: {}", teamId, userId, e);
        }
    }

    @Override
    public void removeUser(String teamId, String userId) {
        try {
            String key = USER_KEY_PREFIX + teamId;
            redisTemplate.opsForHash().delete(key, userId);

            // 활동 정보도 제거
            String activityKey = USER_ACTIVITY_PREFIX + teamId + ":" + userId;
            redisTemplate.delete(activityKey);

            log.debug("사용자 제거 완료 - teamId: {}, userId: {}", teamId, userId);
        } catch (Exception e) {
            log.error("사용자 제거 실패 - teamId: {}, userId: {}", teamId, userId, e);
        }
    }

    @Override
    public void updateUserActivity(String teamId, String userId, String userName) {
        try {
            String activityKey = USER_ACTIVITY_PREFIX + teamId + ":" + userId;
            redisTemplate.opsForValue().set(activityKey, System.currentTimeMillis());
            redisTemplate.expire(activityKey, USER_TTL, TimeUnit.HOURS);

            log.debug("사용자 활동 업데이트 - teamId: {}, userId: {}", teamId, userId);
        } catch (Exception e) {
            log.error("사용자 활동 업데이트 실패 - teamId: {}, userId: {}", teamId, userId, e);
        }
    }

    @Override
    public long getConnectedUserCount(String teamId) {
        try {
            String key = USER_KEY_PREFIX + teamId;
            return redisTemplate.opsForHash().size(key);
        } catch (Exception e) {
            log.error("접속자 수 조회 실패 - teamId: {}", teamId, e);
            return 0;
        }
    }

    @Override
    public Set<Object> getConnectedUsers(String teamId) {
        try {
            String key = USER_KEY_PREFIX + teamId;
            return redisTemplate.opsForHash().keys(key);
        } catch (Exception e) {
            log.error("접속자 목록 조회 실패 - teamId: {}", teamId, e);
            return Set.of();
        }
    }

    @Override
    public boolean isUserBanned(String teamId, String userId) {
        // TODO: 밴 관리 로직 구현
        return false;
    }

    @Override
    public boolean isTeamMember(String teamId, String userId) {
        // TODO: 팀 소속 확인 로직 구현
        return true;
    }
}

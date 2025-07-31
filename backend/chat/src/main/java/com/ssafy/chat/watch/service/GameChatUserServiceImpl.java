package com.ssafy.chat.watch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 게임 채팅 사용자 관리 서비스 구현체 (간단 버전)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameChatUserServiceImpl implements GameChatUserService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String USER_KEY_PREFIX = "game_chat:users:";

    @Override
    public void addUser(String teamId, String userId, String userName) {
        try {
            String key = USER_KEY_PREFIX + teamId;
            redisTemplate.opsForSet().add(key, userId + ":" + userName);
            log.debug("사용자 추가 - teamId: {}, userId: {}", teamId, userId);
        } catch (Exception e) {
            log.error("사용자 추가 실패 - teamId: {}, userId: {}", teamId, userId, e);
        }
    }

    @Override
    public void removeUser(String teamId, String userId) {
        try {
            String key = USER_KEY_PREFIX + teamId;
            Set<Object> users = redisTemplate.opsForSet().members(key);
            
            if (users != null) {
                for (Object user : users) {
                    String userStr = user.toString();
                    if (userStr.startsWith(userId + ":")) {
                        redisTemplate.opsForSet().remove(key, user);
                        break;
                    }
                }
            }
            
            log.debug("사용자 제거 - teamId: {}, userId: {}", teamId, userId);
        } catch (Exception e) {
            log.error("사용자 제거 실패 - teamId: {}, userId: {}", teamId, userId, e);
        }
    }

    @Override
    public long getConnectedUserCount(String teamId) {
        try {
            String key = USER_KEY_PREFIX + teamId;
            Long count = redisTemplate.opsForSet().size(key);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("사용자 수 조회 실패 - teamId: {}", teamId, e);
            return 0;
        }
    }

    @Override
    public List<String> getConnectedUsers(String teamId) {
        try {
            String key = USER_KEY_PREFIX + teamId;
            Set<Object> users = redisTemplate.opsForSet().members(key);
            
            List<String> userList = new ArrayList<>();
            if (users != null) {
                for (Object user : users) {
                    userList.add(user.toString());
                }
            }
            
            return userList;
        } catch (Exception e) {
            log.error("사용자 목록 조회 실패 - teamId: {}", teamId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateUserActivity(String teamId, String userId, String userName) {
        // 간단 버전에서는 활동 시간 업데이트 생략
        log.debug("사용자 활동 업데이트 - teamId: {}, userId: {}", teamId, userId);
    }
}
package com.ssafy.bbatty.domain.chat.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 게임 채팅 트래픽 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameChatTrafficServiceImpl implements GameChatTrafficService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TRAFFIC_KEY_PREFIX = "game_chat:traffic:";
    private static final String HOURLY_TRAFFIC_PREFIX = "game_chat:hourly:";
    private static final long TRAFFIC_TTL = 24; // 24시간

    @Override
    public void incrementTraffic(String teamId) {
        try {
            String key = TRAFFIC_KEY_PREFIX + teamId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, TRAFFIC_TTL, TimeUnit.HOURS);

            // 시간별 트래픽도 증가
            incrementHourlyTraffic(teamId);

            log.debug("트래픽 증가 - teamId: {}", teamId);
        } catch (Exception e) {
            log.error("트래픽 증가 실패 - teamId: {}", teamId, e);
        }
    }

    @Override
    public Long getCurrentTraffic(String teamId) {
        try {
            String key = TRAFFIC_KEY_PREFIX + teamId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.valueOf(value.toString()) : 0L;
        } catch (Exception e) {
            log.error("트래픽 조회 실패 - teamId: {}", teamId, e);
            return 0L;
        }
    }

    /**
     * 시간별 트래픽 증가
     */
    private void incrementHourlyTraffic(String teamId) {
        try {
            String hourKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
            String key = HOURLY_TRAFFIC_PREFIX + teamId + ":" + hourKey;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 25, TimeUnit.HOURS); // 25시간 후 만료
        } catch (Exception e) {
            log.error("시간별 트래픽 증가 실패 - teamId: {}", teamId, e);
        }
    }

    @Override
    public Long getHourlyTraffic(String teamId, String hour) {
        try {
            String key = HOURLY_TRAFFIC_PREFIX + teamId + ":" + hour;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.valueOf(value.toString()) : 0L;
        } catch (Exception e) {
            log.error("시간별 트래픽 조회 실패 - teamId: {}, hour: {}", teamId, hour, e);
            return 0L;
        }
    }

    @Override
    public void resetTraffic(String teamId) {
        try {
            String key = TRAFFIC_KEY_PREFIX + teamId;
            redisTemplate.delete(key);
            log.info("트래픽 리셋 완료 - teamId: {}", teamId);
        } catch (Exception e) {
            log.error("트래픽 리셋 실패 - teamId: {}", teamId, e);
        }
    }

    @Override
    public boolean isTrafficSpike(String teamId, long threshold) {
        try {
            Long currentTraffic = getCurrentTraffic(teamId);
            return currentTraffic != null && currentTraffic > threshold;
        } catch (Exception e) {
            log.error("트래픽 급증 감지 실패 - teamId: {}", teamId, e);
            return false;
        }
    }
}
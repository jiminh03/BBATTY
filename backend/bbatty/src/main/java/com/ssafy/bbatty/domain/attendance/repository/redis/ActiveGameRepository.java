package com.ssafy.bbatty.domain.attendance.repository.redis;

import com.ssafy.bbatty.domain.attendance.dto.redis.ActiveGame;
import com.ssafy.bbatty.global.constants.RedisKey;
import com.ssafy.bbatty.global.util.DateUtil;
import com.ssafy.bbatty.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 활성 경기 Redis 캐시 Repository
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActiveGameRepository {
    
    private final RedisUtil redisUtil;
    
    /**
     * 팀의 당일 활성 경기 목록 캐시에서 조회
     */
    @SuppressWarnings("unchecked")
    public Optional<List<ActiveGame>> findTeamGamesToday(Long teamId, LocalDate date) {
        String key = RedisKey.ATTENDANCE_ACTIVE_GAMES + date + ":" + teamId;
        
        try {
            List<ActiveGame> games = redisUtil.getValue(key, List.class);
            if (games != null && !games.isEmpty()) {
                log.info("Redis 캐시에서 당일 경기 조회 성공: teamId={}, date={}, count={}", 
                        teamId, date, games.size());
                return Optional.of(games);
            }
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패: key={}, error={}", key, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * 팀의 당일 활성 경기 목록을 캐시에 저장
     */
    public void cacheTeamGamesToday(Long teamId, LocalDate date, List<ActiveGame> games) {
        String key = RedisKey.ATTENDANCE_ACTIVE_GAMES + date + ":" + teamId;
        
        try {
            // 당일 자정까지 TTL 설정
            Duration ttl = DateUtil.calculateTTLUntilMidnight();
            redisUtil.setValue(key, games, ttl);
            
            log.info("Redis에 당일 경기 캐시 저장: teamId={}, date={}, count={}, TTL={}초", 
                    teamId, date, games.size(), ttl.getSeconds());
        } catch (Exception e) {
            log.error("Redis 캐시 저장 실패: key={}, error={}", key, e.getMessage());
        }
    }
    
}

package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.domain.statistics.repository.StatisticsRedisRepository;
import com.ssafy.schedule.global.constant.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

/**
 * 통계 업데이트 서비스
 * - 경기 결과 업데이트 후 관련 사용자 통계 재계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsUpdateService {
    
    private final StatisticsRedisRepository statisticsRedisRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 특정 날짜의 직관 인증자들의 통계 재계산
     */
    public void updateUserStatsForDate(String date) {
        log.info("날짜별 사용자 통계 업데이트 시작: date={}", date);
        
        try {
            // 당일 인증자 목록 조회
            String dailyAttendeesKey = RedisKey.ATTENDANCE_DAILY_ATTENDEES + date;
            Set<Object> members = redisTemplate.opsForSet().members(dailyAttendeesKey);
            
            if (members == null || members.isEmpty()) {
                log.info("당일 직관 인증자가 없음: date={}", date);
                return;
            }
            
            log.info("당일 직관 인증자 {}명 발견: date={}", members.size(), date);
            
            // 현재 시즌 가져오기
            String currentSeason = String.valueOf(LocalDate.now().getYear());
            
            // 각 사용자의 통계 캐시 무효화
            for (Object memberObj : members) {
                try {
                    String userIdStr = String.valueOf(memberObj);
                    Long userId = Long.valueOf(userIdStr);
                    statisticsRedisRepository.clearCurrentSeasonAndTotalStats(userId, currentSeason);
                    log.debug("사용자 통계 캐시 삭제: userId={}, currentSeason={}", userId, currentSeason);
                } catch (NumberFormatException e) {
                    log.warn("잘못된 userId 형식: {}", memberObj);
                }
            }
            
        } catch (Exception e) {
            log.error("사용자 통계 업데이트 실패: date={}", date, e);
        }
    }
}
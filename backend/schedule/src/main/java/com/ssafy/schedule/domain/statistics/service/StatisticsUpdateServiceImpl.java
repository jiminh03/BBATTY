package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.domain.statistics.service.RankingUpdateService;
import com.ssafy.schedule.domain.statistics.repository.StatisticsRedisRepository;
import com.ssafy.schedule.global.constant.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 통계 업데이트 서비스 구현체
 * - 경기 결과 업데이트 후 관련 사용자 통계 재계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsUpdateServiceImpl implements StatisticsUpdateService {
    
    private final StatisticsRedisRepository statisticsRedisRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StatisticsService statisticsService;
    private final RankingUpdateService rankingUpdateService;
    
    @Override
    public void updateUserStatsForDate(String date) {
        log.info("날짜별 사용자 통계 업데이트 시작: date={}", date);
        
        try {
            // 당일 인증자 목록 조회 (Hash에서 userId:teamId 형태로)
            String dailyAttendeesKey = RedisKey.ATTENDANCE_DAILY_ATTENDEES + date;
            java.util.Map<Object, Object> attendeesMap = redisTemplate.opsForHash().entries(dailyAttendeesKey);
            
            if (attendeesMap == null || attendeesMap.isEmpty()) {
                log.info("당일 직관 인증자가 없음: date={}", date);
                return;
            }
            
            log.info("당일 직관 인증자 {}명 발견: date={}", attendeesMap.size(), date);
            
            // 현재 시즌 가져오기
            String currentSeason = String.valueOf(LocalDate.now().getYear());
            
            // 각 사용자의 통계 즉시 재계산 (캐시 무효화 + 재계산)
            for (java.util.Map.Entry<Object, Object> entry : attendeesMap.entrySet()) {
                try {
                    String userIdStr = String.valueOf(entry.getKey());
                    String teamIdStr = String.valueOf(entry.getValue());
                    Long userId = Long.valueOf(userIdStr);
                    Long teamId = Long.valueOf(teamIdStr);
                    
                    // 기존 캐시 삭제
                    statisticsRedisRepository.clearCurrentSeasonAndTotalStats(userId, currentSeason);
                    
                    // 즉시 통계 재계산
                    var basicStats = statisticsService.calculateUserBasicStats(userId, currentSeason, teamId);
                    statisticsService.calculateUserDetailedStats(userId, currentSeason, teamId);
                    statisticsService.calculateUserStreakStats(userId, teamId);
                    
                    // 이번 시즌 10경기 이상일 때 랭킹 업데이트
                    if (basicStats.getTotalGames() >= 10) {
                        rankingUpdateService.updateUserWinRateRanking(userId, teamId, Double.parseDouble(basicStats.getWinRate()));
                    }
                    
                    log.debug("사용자 통계 재계산 완료: userId={}, teamId={}, currentSeason={}, totalGames={}", userId, teamId, currentSeason, basicStats.getTotalGames());
                } catch (NumberFormatException e) {
                    log.warn("잘못된 userId/teamId 형식: userId={}, teamId={}", entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    log.error("사용자 통계 재계산 실패: userId={}, teamId={}", entry.getKey(), entry.getValue(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("사용자 통계 업데이트 실패: date={}", date, e);
        }
    }
}
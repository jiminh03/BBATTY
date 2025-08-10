package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.domain.statistics.repository.StatisticsRedisRepository;
import com.ssafy.schedule.global.constants.RedisKey;
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
    
    private static final int MINIMUM_GAMES_FOR_RANKING = 10;
    
    private final StatisticsRedisRepository statisticsRedisRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StatisticsService statisticsService;
    private final RankingUpdateService rankingUpdateService;
    private final BadgeUpdateService badgeUpdateService;
    
    @Override
    public void updateUserStatsForDate(String date) {
        log.info("날짜별 사용자 통계 업데이트 시작: date={}", date);
        
        try {
            // 당일 인증자 목록 조회 (Hash에서 userId:teamId:gameId 형태로)
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
                    String teamGameValue = String.valueOf(entry.getValue());
                    
                    // teamId:gameId 파싱
                    String[] parts = teamGameValue.split(":");
                    if (parts.length != 2) {
                        log.warn("잘못된 teamId:gameId 형식: userId={}, value={}", userIdStr, teamGameValue);
                        continue;
                    }
                    
                    Long userId = Long.valueOf(userIdStr);
                    Long teamId = Long.valueOf(parts[0]);
                    Long gameId = Long.valueOf(parts[1]);
                    
                    // 통계/랭킹/뱃지 업데이트를 안전하게 처리
                    updateUserStats(userId, teamId, gameId, currentSeason);
                    
                    log.debug("사용자 통계 재계산 완료: userId={}, teamId={}, gameId={}, currentSeason={}", 
                        userId, teamId, gameId, currentSeason);
                } catch (NumberFormatException e) {
                    log.warn("잘못된 숫자 형식: userId={}, value={}", entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    log.error("사용자 개별 통계 재계산 실패: userId={}, value={}", entry.getKey(), entry.getValue(), e);
                    // 개별 사용자 실패해도 다른 사용자 처리는 계속
                }
            }
            
        } catch (Exception e) {
            log.error("사용자 통계 업데이트 실패: date={}", date, e);
        }
    }
    
    /**
     * 사용자 통계/랭킹/뱃지를 안전하게 업데이트
     * 각 단계별로 예외 처리하여 부분 실패 시에도 다른 업데이트는 계속 진행
     */
    private void updateUserStats(Long userId, Long teamId, Long gameId, String currentSeason) {
        boolean statisticsUpdated = false;
        boolean rankingUpdated = false;
        boolean badgeUpdated = false;
        
        try {
            // 1. 기존 캐시 삭제 및 통계 재계산
            statisticsRedisRepository.clearCurrentSeasonAndTotalStats(userId, currentSeason);
            var basicStats = statisticsService.calculateUserBasicStats(userId, currentSeason, teamId);
            statisticsService.calculateUserDetailedStats(userId, currentSeason, teamId);
            statisticsService.calculateUserStreakStats(userId, teamId);
            statisticsUpdated = true;
            
            // 2. 랭킹 업데이트 (최소 경기 수 이상일 때만)
            if (basicStats.getTotalGames() >= MINIMUM_GAMES_FOR_RANKING) {
                try {
                    rankingUpdateService.updateUserWinRateRanking(userId, teamId, Double.parseDouble(basicStats.getWinRate()));
                    rankingUpdated = true;
                } catch (Exception e) {
                    log.warn("랭킹 업데이트 실패 (통계는 성공): userId={}, teamId={}", userId, teamId, e);
                }
            }
            
            // 3. 뱃지 업데이트
            try {
                badgeUpdateService.updateBadgesOnGameResult(userId, gameId, currentSeason);
                badgeUpdated = true;
            } catch (Exception e) {
                log.warn("뱃지 업데이트 실패 (통계는 성공): userId={}, gameId={}", userId, gameId, e);
            }
            
            log.debug("사용자 업데이트 완료: userId={}, stats={}, ranking={}, badge={}", 
                userId, statisticsUpdated, rankingUpdated, badgeUpdated);
                
        } catch (Exception e) {
            log.error("사용자 통계 업데이트 전체 실패: userId={}, teamId={}, gameId={}", userId, teamId, gameId, e);
        }
    }
}
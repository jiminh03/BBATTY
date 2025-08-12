package com.ssafy.schedule.domain.statistics.controller;

import com.ssafy.schedule.domain.statistics.dto.response.UserBasicStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserDetailedStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserStreakStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserRankingResponse;
import com.ssafy.schedule.domain.statistics.service.StatisticsService;
import com.ssafy.schedule.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    /**
     * 모든 사용자 통계 일괄 계산 및 캐싱 (개발용)
     */
    @PostMapping("/calculate-all")
    public ResponseEntity<Map<String, Object>> calculateAllUserStats(
            @RequestParam(required = false, defaultValue = "2025") String season) {
        
        log.info("모든 사용자 통계 일괄 계산 요청 - season: {}", season);
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            // 더미데이터에 있는 모든 사용자 ID들 (1번부터 137번까지)
            List<Long> userIds = new ArrayList<>();
            for (long i = 1L; i <= 137L; i++) {
                userIds.add(i);
            }
            
            int totalUsers = userIds.size();
            int successCount = 0;
            int failCount = 0;
            
            log.info("직관 기록이 있는 사용자 {}명 발견", totalUsers);
            
            for (Long userId : userIds) {
                try {
                    log.info("사용자 {} 통계 계산 시작", userId);
                    
                    // BBATTY 서비스에서 사용자의 실제 팀 ID 조회
                    Long teamId = getUserTeamId(userId);
                    if (teamId == null) {
                        log.warn("사용자 {}의 팀 정보를 찾을 수 없습니다. 건너뛰기", userId);
                        continue;
                    }
                    
                    // 기본 통계 계산
                    UserBasicStatsResponse basicStats = statisticsService.calculateUserBasicStats(
                            userId, season, teamId);
                    
                    // 상세 통계 계산
                    UserDetailedStatsResponse detailedStats = statisticsService.calculateUserDetailedStats(
                            userId, season, teamId);
                    
                    // 연승 통계 계산
                    UserStreakStatsResponse streakStats = statisticsService.calculateUserStreakStats(
                            userId, season, teamId);
                    
                    Map<String, Object> userResult = new HashMap<>();
                    userResult.put("userId", userId);
                    userResult.put("teamId", teamId);
                    userResult.put("success", true);
                    userResult.put("basicStats", basicStats);
                    userResult.put("detailedStats", detailedStats);
                    userResult.put("streakStats", streakStats);
                    
                    results.add(userResult);
                    successCount++;
                    
                    log.info("사용자 {} 통계 계산 완료", userId);
                    
                } catch (Exception e) {
                    log.error("사용자 {} 통계 계산 실패: {}", userId, e.getMessage());
                    
                    Map<String, Object> userResult = new HashMap<>();
                    userResult.put("userId", userId);
                    userResult.put("success", false);
                    userResult.put("error", e.getMessage());
                    
                    results.add(userResult);
                    failCount++;
                }
            }
            
            // 승률 랭킹 계산 및 캐싱
            calculateAndCacheRankings(season, results);
            
            // 뱃지 정보 캐싱
            calculateAndCacheBadges(season, results);
            
            response.put("success", true);
            response.put("message", "모든 사용자 통계, 랭킹 및 뱃지 일괄 캐싱 완료");
            response.put("season", season);
            response.put("totalUsers", totalUsers);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("results", results);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("모든 사용자 통계 일괄 계산 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "일괄 계산 실패: " + e.getMessage());
            response.put("results", results);
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    

    /**
     * 승률 랭킹 계산 및 캐싱 (bbatty 서버 호환 방식)
     */
    private void calculateAndCacheRankings(String season, List<Map<String, Object>> results) {
        log.info("승률 랭킹 계산 시작 - season: {}", season);
        
        try {
            // 성공한 결과만 필터링하여 랭킹 계산
            List<UserRankingResponse> allRankings = results.stream()
                    .filter(result -> (Boolean) result.get("success"))
                    .map(result -> {
                        UserBasicStatsResponse basicStats = (UserBasicStatsResponse) result.get("basicStats");
                        Long userId = (Long) result.get("userId");
                        Long teamId = (Long) result.get("teamId");
                        
                        return UserRankingResponse.builder()
                                .userId(userId)
                                .teamId(teamId)
                                .totalGames(basicStats.getTotalGames())
                                .wins(basicStats.getWins())
                                .losses(basicStats.getLosses())
                                .winRate(basicStats.getWinRate())
                                .build();
                    })
                    .filter(ranking -> ranking.getTotalGames() >= 5) // 최소 5경기 이상만 랭킹 포함
                    .sorted((r1, r2) -> {
                        // 승률로 내림차순 정렬, 동일하면 총 경기수로 내림차순
                        double winRate1 = Double.parseDouble(r1.getWinRate());
                        double winRate2 = Double.parseDouble(r2.getWinRate());
                        int compare = Double.compare(winRate2, winRate1);
                        if (compare == 0) {
                            return Integer.compare(r2.getTotalGames(), r1.getTotalGames());
                        }
                        return compare;
                    })
                    .collect(Collectors.toList());
            
            // 전체 랭킹 TOP 10 Redis ZSet에 저장 (bbatty 서버 호환)
            String globalRankingKey = "ranking:global:top10";
            String globalAllRankingKey = "ranking:global:all";
            redisTemplate.delete(globalRankingKey); // 기존 데이터 삭제
            redisTemplate.delete(globalAllRankingKey); // 기존 전체 랭킹 데이터 삭제
            
            // 전체 사용자 랭킹 저장 (모든 사용자)
            for (UserRankingResponse ranking : allRankings) {
                double winRate = Double.parseDouble(ranking.getWinRate());
                redisTemplate.opsForZSet().add(globalAllRankingKey, ranking.getUserId().toString(), winRate);
            }
            log.info("전체 사용자 랭킹 캐싱 완료 - key: {}, count: {}", globalAllRankingKey, allRankings.size());
            
            // TOP 10만 따로 저장
            int globalTop10Count = Math.min(allRankings.size(), 10);
            for (int i = 0; i < globalTop10Count; i++) {
                UserRankingResponse ranking = allRankings.get(i);
                double winRate = Double.parseDouble(ranking.getWinRate());
                redisTemplate.opsForZSet().add(globalRankingKey, ranking.getUserId().toString(), winRate);
            }
            log.info("전체 랭킹 TOP 10 캐싱 완료 - key: {}, count: {}", globalRankingKey, globalTop10Count);
            
            // 팀별 랭킹 TOP 10 Redis ZSet에 저장 (bbatty 서버 호환)
            Map<Long, List<UserRankingResponse>> teamRankings = allRankings.stream()
                    .collect(Collectors.groupingBy(UserRankingResponse::getTeamId));
            
            teamRankings.forEach((teamId, rankings) -> {
                // 팀별 승률로 재정렬
                rankings.sort((r1, r2) -> {
                    double winRate1 = Double.parseDouble(r1.getWinRate());
                    double winRate2 = Double.parseDouble(r2.getWinRate());
                    int compare = Double.compare(winRate2, winRate1);
                    if (compare == 0) {
                        return Integer.compare(r2.getTotalGames(), r1.getTotalGames());
                    }
                    return compare;
                });
                
                String teamRankingKey = "ranking:team:" + teamId + ":top10";
                String teamAllRankingKey = "ranking:team:" + teamId + ":all";
                redisTemplate.delete(teamRankingKey); // 기존 데이터 삭제
                redisTemplate.delete(teamAllRankingKey); // 기존 전체 팀 랭킹 데이터 삭제
                
                // 전체 팀원 랭킹 저장 (모든 팀원)
                for (UserRankingResponse ranking : rankings) {
                    double winRate = Double.parseDouble(ranking.getWinRate());
                    redisTemplate.opsForZSet().add(teamAllRankingKey, ranking.getUserId().toString(), winRate);
                }
                log.info("전체 팀원 랭킹 캐싱 완료 - key: {}, count: {}", teamAllRankingKey, rankings.size());
                
                // TOP 10만 따로 저장
                int teamTop10Count = Math.min(rankings.size(), 10);
                for (int i = 0; i < teamTop10Count; i++) {
                    UserRankingResponse ranking = rankings.get(i);
                    double winRate = Double.parseDouble(ranking.getWinRate());
                    redisTemplate.opsForZSet().add(teamRankingKey, ranking.getUserId().toString(), winRate);
                }
                log.info("팀별 랭킹 TOP 10 캐싱 완료 - key: {}, count: {}", teamRankingKey, teamTop10Count);
            });
            
        } catch (Exception e) {
            log.error("랭킹 계산 및 캐싱 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 뱃지 정보 계산 및 캐싱 (bbatty 서버 호환 방식)
     */
    private void calculateAndCacheBadges(String season, List<Map<String, Object>> results) {
        log.info("뱃지 정보 계산 및 캐싱 시작 - season: {}", season);
        
        try {
            results.stream()
                .filter(result -> (Boolean) result.get("success"))
                .forEach(result -> {
                    Long userId = (Long) result.get("userId");
                    Long teamId = (Long) result.get("teamId");
                    UserBasicStatsResponse basicStats = (UserBasicStatsResponse) result.get("basicStats");
                    UserDetailedStatsResponse detailedStats = (UserDetailedStatsResponse) result.get("detailedStats");
                    
                    try {
                        // 시즌 승리 뱃지 캐싱
                        cacheSeasonWinsBadges(userId, season, basicStats.getWins());
                        
                        // 시즌 직관 뱃지 캐싱
                        cacheSeasonGamesBadges(userId, season, basicStats.getTotalGames());
                        
                        // 구장 뱃지 캐싱 (상세 통계에서 구장별 정보 활용)
                        cacheStadiumBadges(userId, detailedStats);
                        
                        log.debug("사용자 {} 뱃지 정보 캐싱 완료", userId);
                        
                    } catch (Exception e) {
                        log.error("사용자 {} 뱃지 캐싱 실패: {}", userId, e.getMessage());
                    }
                });
                
        } catch (Exception e) {
            log.error("뱃지 정보 캐싱 실패: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 시즌 승리 뱃지 캐싱
     */
    private void cacheSeasonWinsBadges(Long userId, String season, Integer wins) {
        String timestamp = java.time.LocalDateTime.now().toString();
        
        // BadgeType enum에 정의된 승리 뱃지 단계 (1승, 5승, 15승)
        int[] winMilestones = {1, 5, 15};
        
        for (int milestone : winMilestones) {
            if (wins >= milestone) {
                String key = "badge:wins:" + userId + ":" + season + ":" + milestone;
                redisTemplate.opsForValue().set(key, timestamp);
                log.info("승리 뱃지 캐싱: {} - {}승 (실제승수: {})", userId, milestone, wins);
            }
        }
    }
    
    /**
     * 시즌 직관 뱃지 캐싱
     */
    private void cacheSeasonGamesBadges(Long userId, String season, Integer totalGames) {
        String timestamp = java.time.LocalDateTime.now().toString();
        
        // BadgeType enum에 정의된 직관 뱃지 단계 (1경기, 10경기, 30경기)
        int[] gameMilestones = {1, 10, 30};
        
        for (int milestone : gameMilestones) {
            if (totalGames >= milestone) {
                String key = "badge:games:" + userId + ":" + season + ":" + milestone;
                redisTemplate.opsForValue().set(key, timestamp);
                log.info("직관 뱃지 캐싱: {} - {}경기 (실제경기수: {})", userId, milestone, totalGames);
            }
        }
    }
    
    /**
     * 구장 뱃지 캐싱
     */
    private void cacheStadiumBadges(Long userId, UserDetailedStatsResponse detailedStats) {
        String timestamp = java.time.LocalDateTime.now().toString();
        
        // 구장별 통계에서 최소 1경기 이상 직관한 구장에 대해 뱃지 획득
        if (detailedStats.getStadiumStats() != null) {
            Map<String, UserDetailedStatsResponse.CategoryStats> stadiumStats = detailedStats.getStadiumStats();
            
            stadiumStats.forEach((stadiumName, stats) -> {
                if (stats != null && stats.getGames() > 0) {
                    // 구장명을 영어로 매핑 (실제 Stadium enum과 매칭)
                    String stadiumKey = mapStadiumNameToKey(stadiumName);
                    if (stadiumKey != null) {
                        String key = "badge:stadium:" + userId + ":" + stadiumKey;
                        redisTemplate.opsForValue().set(key, timestamp);
                        log.info("구장 뱃지 캐싱: {} - {} ({}경기)", userId, stadiumKey, stats.getGames());
                    }
                }
            });
        }
    }
    
    /**
     * 구장명을 Stadium enum 키로 매핑
     */
    private String mapStadiumNameToKey(String stadiumName) {
        return switch (stadiumName) {
            case "잠실야구장" -> "JAMSIL";
            case "고척스카이돔" -> "GOCHEOK";
            case "인천SSG랜더스필드" -> "INCHEON";
            case "대구삼성라이온즈파크" -> "DAEGU";
            case "광주기아챔피언스필드" -> "GWANGJU";
            case "대전한화생명볼파크" -> "DAEJEON";
            case "수원KT위즈파크" -> "SUWON";
            case "창원NC파크" -> "CHANGWON";
            case "부산사직야구장" -> "BUSAN";
            case "포항야구장" -> "POHANG";
            default -> null;
        };
    }

    /**
     * 사용자의 실제 응원팀 ID 조회 (DB에서 조회)
     */
    private Long getUserTeamId(Long userId) {
        return userRepository.findTeamIdByUserId(userId).orElse(null);
    }

    /**
     * 사용자의 닉네임 조회 (DB에서 조회)
     */
    private String getUserNickname(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getNickname())
                .orElse("사용자" + userId);
    }
}
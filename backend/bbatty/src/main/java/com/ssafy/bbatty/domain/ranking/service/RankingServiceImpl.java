package com.ssafy.bbatty.domain.ranking.service;

import com.ssafy.bbatty.domain.ranking.dto.response.GlobalRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.TeamRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.UserRankingDto;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.global.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final TeamRepository teamRepository;
    
    
    @Override
    public GlobalRankingResponse getGlobalWinRateRankingWithMyRank(Long currentUserId) {
        log.info("전체 승률 랭킹 + 내 순위 조회 시작: userId={}", currentUserId);
        
        String globalRankingKey = RedisKey.RANKING_GLOBAL_TOP10;
        
        Set<ZSetOperations.TypedTuple<Object>> rankingData = redisTemplate.opsForZSet()
                .reverseRangeWithScores(globalRankingKey, 0, 9);
        
        if (rankingData == null || rankingData.isEmpty()) {
            log.info("전체 랭킹 데이터 없음");
            return GlobalRankingResponse.builder()
                    .season(getCurrentSeason())
                    .rankings(new ArrayList<>())
                    .build();
        }
        
        List<UserRankingDto> rankings = new ArrayList<>();
        AtomicInteger rank = new AtomicInteger(1);
        boolean foundCurrentUser = false;
        
        for (ZSetOperations.TypedTuple<Object> tuple : rankingData) {
            Long userId = Long.valueOf(tuple.getValue().toString());
            Double winRate = tuple.getScore();
            boolean isCurrentUser = userId.equals(currentUserId);
            
            if (isCurrentUser) {
                foundCurrentUser = true;
            }
            
            rankings.add(UserRankingDto.builder()
                    .userId(userId)
                    .winRate(winRate)
                    .rank(rank.getAndIncrement())
                    .percentile(isCurrentUser ? calculatePercentile(globalRankingKey, currentUserId) : null)
                    .isCurrentUser(isCurrentUser)
                    .build());
        }
        
        UserRankingDto myRanking = null;
        if (!foundCurrentUser) {
            myRanking = getMyRankingFromRedis(globalRankingKey, currentUserId);
        }
        
        log.info("전체 승률 랭킹 + 내 순위 조회 완료: {} 명", rankings.size());
        
        return GlobalRankingResponse.builder()
                .season(getCurrentSeason())
                .rankings(rankings)
                .myRanking(myRanking)
                .build();
    }
    
    
    @Override
    public TeamRankingResponse getTeamWinRateRankingWithMyRank(Long teamId, Long currentUserId, Long currentUserTeamId) {
        log.info("팀별 승률 랭킹 조회 시작: teamId={}, userId={}", teamId, currentUserId);
        
        String teamRankingKey = RedisKey.RANKING_TEAM_TOP10 + teamId + ":top10";
        
        Set<ZSetOperations.TypedTuple<Object>> rankingData = redisTemplate.opsForZSet()
                .reverseRangeWithScores(teamRankingKey, 0, 9);
        
        if (rankingData == null || rankingData.isEmpty()) {
            log.info("팀별 랭킹 데이터 없음: teamId={}", teamId);
            Optional<Team> teamOpt = teamRepository.findById(teamId);
            String teamName = teamOpt.map(Team::getName).orElse("Unknown Team");
            
            return TeamRankingResponse.builder()
                    .teamId(teamId)
                    .teamName(teamName)
                    .season(getCurrentSeason())
                    .rankings(new ArrayList<>())
                    .build();
        }
        
        boolean isMyTeam = teamId.equals(currentUserTeamId);
        
        List<UserRankingDto> rankings = new ArrayList<>();
        AtomicInteger rank = new AtomicInteger(1);
        boolean foundCurrentUser = false;
        
        for (ZSetOperations.TypedTuple<Object> tuple : rankingData) {
            Long userId = Long.valueOf(tuple.getValue().toString());
            Double winRate = tuple.getScore();
            boolean isCurrentUser = userId.equals(currentUserId);
            
            if (isCurrentUser && isMyTeam) {
                foundCurrentUser = true;
            }
            
            rankings.add(UserRankingDto.builder()
                    .userId(userId)
                    .winRate(winRate)
                    .rank(rank.getAndIncrement())
                    .percentile((isCurrentUser && isMyTeam) ? calculatePercentile(teamRankingKey, currentUserId) : null)
                    .isCurrentUser(isCurrentUser && isMyTeam)
                    .build());
        }
        
        UserRankingDto myRanking = null;
        if (!foundCurrentUser && isMyTeam) {
            myRanking = getMyRankingFromRedis(teamRankingKey, currentUserId);
        }
        
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        String teamName = teamOpt.map(Team::getName).orElse("Unknown Team");
        
        log.info("팀별 승률 랭킹 조회 완료: teamId={}, {} 명, isMyTeam={}", teamId, rankings.size(), isMyTeam);
        
        return TeamRankingResponse.builder()
                .teamId(teamId)
                .teamName(teamName)
                .season(getCurrentSeason())
                .rankings(rankings)
                .myRanking(myRanking)
                .build();
    }
    
    
    private Double calculatePercentile(String rankingKey, Long userId) {
        Long userRank = redisTemplate.opsForZSet().reverseRank(rankingKey, userId.toString());
        if (userRank == null) return null;
        
        Long totalUsers = redisTemplate.opsForZSet().zCard(rankingKey);
        if (totalUsers == null || totalUsers <= 1) return null;
        
        // 상위 백분위 계산: (실제 순위 / 전체 사용자 수) * 100
        int actualRank = userRank.intValue() + 1;
        return ((double) actualRank / totalUsers) * 100.0;
    }
    
    /**
     * 전체 랭킹에서 백분위 계산
     */
    private Double calculatePercentileFromAllRanking(String rankingKey, Long userId) {
        String allRankingKey = getComprehensiveRankingKey(rankingKey);
        
        Long userRank = redisTemplate.opsForZSet().reverseRank(allRankingKey, userId.toString());
        if (userRank == null) return null;
        
        Long totalUsers = redisTemplate.opsForZSet().zCard(allRankingKey);
        if (totalUsers == null || totalUsers <= 1) return null;
        
        // 백분위 계산: 상위 몇 %인지 계산
        // userRank는 0-based이므로 실제 순위 = userRank + 1
        // 상위 백분위 = (실제 순위 / 전체 사용자 수) * 100
        // 예: 전체 10명 중 4위 → (4 / 10) * 100 = 40% (상위 40%)
        int actualRank = userRank.intValue() + 1;
        return ((double) actualRank / totalUsers) * 100.0;
    }
    
    /**
     * TOP 10 랭킹 키를 전체 랭킹 키로 변환
     */
    private String getComprehensiveRankingKey(String top10Key) {
        if (top10Key.equals(RedisKey.RANKING_GLOBAL_TOP10)) {
            return "ranking:global:all";
        } else if (top10Key.startsWith(RedisKey.RANKING_TEAM_TOP10)) {
            // "ranking:team:{teamId}:top10" -> "ranking:team:{teamId}:all"
            return top10Key.replace(":top10", ":all");
        }
        return top10Key;
    }
    
    private UserRankingDto getMyRankingFromRedis(String rankingKey, Long userId) {
        // 항상 전체 랭킹에서 조회 (정확한 순위를 위해)
        String allRankingKey = getComprehensiveRankingKey(rankingKey);
        Double winRate = redisTemplate.opsForZSet().score(allRankingKey, userId.toString());
        
        if (winRate == null) return null;
        
        Long rank = redisTemplate.opsForZSet().reverseRank(allRankingKey, userId.toString());
        if (rank == null) return null;
        
        return UserRankingDto.builder()
                .userId(userId)
                .winRate(winRate)
                .rank((int) (rank + 1))
                .percentile(calculatePercentileFromAllRanking(rankingKey, userId))
                .isCurrentUser(true)
                .build();
    }

    private String getCurrentSeason() {
        return String.valueOf(LocalDate.now().getYear());
    }
}
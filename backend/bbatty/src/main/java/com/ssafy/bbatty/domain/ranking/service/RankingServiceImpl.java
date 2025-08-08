package com.ssafy.bbatty.domain.ranking.service;

import com.ssafy.bbatty.domain.ranking.dto.response.GlobalRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.TeamRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.UserRankingDto;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    
    @Override
    public GlobalRankingResponse getGlobalWinRateRanking() {
        log.info("전체 승률 랭킹 조회 시작");
        
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
        
        for (ZSetOperations.TypedTuple<Object> tuple : rankingData) {
            Long userId = Long.valueOf(tuple.getValue().toString());
            Double winRate = tuple.getScore();
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                rankings.add(UserRankingDto.builder()
                        .userId(userId)
                        .nickname(user.getNickname())
                        .winRate(winRate)
                        .rank(rank.getAndIncrement())
                        .build());
            }
        }
        
        log.info("전체 승률 랭킹 조회 완료: {} 명", rankings.size());
        
        return GlobalRankingResponse.builder()
                .season(getCurrentSeason())
                .rankings(rankings)
                .build();
    }
    
    @Override
    public TeamRankingResponse getTeamWinRateRanking(Long teamId) {
        log.info("팀별 승률 랭킹 조회 시작: teamId={}", teamId);
        
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
        
        List<UserRankingDto> rankings = new ArrayList<>();
        AtomicInteger rank = new AtomicInteger(1);
        
        for (ZSetOperations.TypedTuple<Object> tuple : rankingData) {
            Long userId = Long.valueOf(tuple.getValue().toString());
            Double winRate = tuple.getScore();
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                rankings.add(UserRankingDto.builder()
                        .userId(userId)
                        .nickname(user.getNickname())
                        .winRate(winRate)
                        .rank(rank.getAndIncrement())
                        .build());
            }
        }
        
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        String teamName = teamOpt.map(Team::getName).orElse("Unknown Team");
        
        log.info("팀별 승률 랭킹 조회 완료: teamId={}, {} 명", teamId, rankings.size());
        
        return TeamRankingResponse.builder()
                .teamId(teamId)
                .teamName(teamName)
                .season(getCurrentSeason())
                .rankings(rankings)
                .build();
    }
    
    private String getCurrentSeason() {
        return String.valueOf(LocalDate.now().getYear());
    }
}
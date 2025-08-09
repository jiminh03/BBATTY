package com.ssafy.bbatty.domain.team.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.team.dto.response.TeamRankingResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis 키 상수 (schedule 서비스와 동일)
    private static final String CURRENT_RANKING_KEY = "team:ranking:2025:current";

    @Override
    public List<TeamRankingResponseDto> getTeamRanking() {
        try {
            // Redis에서 캐시된 순위 조회
            String rankingJson = redisTemplate.opsForValue().get(CURRENT_RANKING_KEY);

            if (rankingJson == null) {
                log.warn("Redis에 캐시된 팀 순위 데이터가 없습니다.");
                return Collections.emptyList();
            }

            // JSON을 TeamStats 리스트로 역직렬화
            List<TeamStats> teamStats = objectMapper.readValue(rankingJson, new TypeReference<List<TeamStats>>() {});

            // TeamStats를 TeamRankingResponseDto로 변환
            return teamStats.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("팀 순위 조회 실패", e);
            return Collections.emptyList();
        }
    }

    private TeamRankingResponseDto convertToResponseDto(TeamStats stats) {
        return new TeamRankingResponseDto(
                stats.getTeamName(),
                stats.getGames(),
                stats.getWins(),
                stats.getDraws(),
                stats.getLosses(),
                stats.getWinRate(),
                stats.getGameBehind(),
                stats.getStreak()
        );
    }

    /**
     * Redis에서 받아올 TeamStats 클래스 (schedule 서비스의 TeamStats와 동일한 구조)
     */
    public static class TeamStats {
        private String teamName;
        private int games;
        private int wins;
        private int draws;
        private int losses;
        private double gameBehind;
        private int streak;

        // 기본 생성자 (Jackson 역직렬화용)
        public TeamStats() {}

        // Getters
        public String getTeamName() { return teamName; }
        public int getGames() { return games; }
        public int getWins() { return wins; }
        public int getDraws() { return draws; }
        public int getLosses() { return losses; }
        public double getGameBehind() { return gameBehind; }
        public int getStreak() { return streak; }

        // 승률 계산
        public double getWinRate() {
            int decisiveGames = wins + losses;
            if (decisiveGames == 0) {
                return 0.0;
            }
            return (double) wins / decisiveGames;
        }

        // Setters (Jackson 역직렬화용)
        public void setTeamName(String teamName) { this.teamName = teamName; }
        public void setGames(int games) { this.games = games; }
        public void setWins(int wins) { this.wins = wins; }
        public void setDraws(int draws) { this.draws = draws; }
        public void setLosses(int losses) { this.losses = losses; }
        public void setGameBehind(double gameBehind) { this.gameBehind = gameBehind; }
        public void setStreak(int streak) { this.streak = streak; }
    }
}

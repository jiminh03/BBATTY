package com.ssafy.schedule.domain.crawler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.schedule.global.constants.GameResult;
import com.ssafy.schedule.global.constants.GameStatus;
import com.ssafy.schedule.global.entity.Game;
import com.ssafy.schedule.global.entity.Team;
import com.ssafy.schedule.global.repository.GameRepository;
import com.ssafy.schedule.global.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 구단 순위 계산 서비스
 * - 2025년 완료된 경기를 바탕으로 구단별 승/무/패/총경기수 계산
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TeamRankingService {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis 키 상수
    private static final String CURRENT_RANKING_KEY = "team:ranking:2025:current";

    /**
     * 구단 순위 통계 클래스
     */
    public static class TeamStats {
        private final String teamName;
        private int games = 0;          // 총 경기수
        private int wins = 0;           // 승
        private int draws = 0;          // 무
        private int losses = 0;         // 패
        private double gameBehind = 0.0; // 경기차
        private int streak = 0;         // 연승(+)/연패(-)

        public TeamStats(String teamName) {
            this.teamName = teamName;
        }

        // Getters
        public String getTeamName() { return teamName; }
        public int getGames() { return games; }
        public int getWins() { return wins; }
        public int getDraws() { return draws; }
        public int getLosses() { return losses; }
        public double getGameBehind() { return gameBehind; }
        public int getStreak() { return streak; }
        
        // 승률 계산 (승수 / (승 + 패))
        public double getWinRate() {
            int decisiveGames = wins + losses;
            if (decisiveGames == 0) {
                return 0.0;
            }
            return (double) wins / decisiveGames;
        }
        
        // 경기차 설정
        public void setGameBehind(double gameBehind) {
            this.gameBehind = gameBehind;
        }
        
        // 연승/연패 설정
        public void setStreak(int streak) {
            this.streak = streak;
        }

        // 경기 결과 추가 (해당 팀이 승리했는지 여부)
        public void addWin() {
            games++;
            wins++;
        }
        
        public void addDraw() {
            games++;
            draws++;
        }
        
        public void addLoss() {
            games++;
            losses++;
        }

        @Override
        public String toString() {
            String streakText = streak > 0 ? streak + "연승" : 
                               streak < 0 ? Math.abs(streak) + "연패" : "-";
            return String.format("%s: %d경기 %d승%d무%d패 (승률 %.3f, 경기차 %.1f, %s)", 
                    teamName, games, wins, draws, losses, getWinRate(), gameBehind, streakText);
        }
    }

    /**
     * 2025년 완료된 경기의 구단 통계를 계산
     * 
     * @return 구단별 승/무/패/총경기수 통계 리스트
     */
    @Transactional(readOnly = true)
    public List<TeamStats> calculateTeamRanking() {
        log.info("2025년 구단 통계 계산 시작");

        // 1. 2025년 완료된 경기 조회
        List<Game> finishedGames = gameRepository.findByStatusAndYear(GameStatus.FINISHED, 2025);
        log.info("2025년 완료된 경기 수: {}경기", finishedGames.size());

        return calculateRankingFromGames(finishedGames, "2025년 전체");
    }

    /**
     * 경기 목록으로부터 구단 통계를 계산하는 공통 메서드
     */
    private List<TeamStats> calculateRankingFromGames(List<Game> finishedGames, String period) {

        if (finishedGames.isEmpty()) {
            log.warn("{} 완료된 경기가 없습니다.", period);
            return Collections.emptyList();
        }

        // 2. 모든 팀 조회
        List<Team> allTeams = teamRepository.findAll();
        
        // 3. 팀별 통계 초기화
        Map<String, TeamStats> teamStatsMap = allTeams.stream()
                .collect(Collectors.toMap(
                        Team::getName,
                        team -> new TeamStats(team.getName())
                ));

        // 4. 경기 결과 집계
        for (Game game : finishedGames) {
            String homeTeamName = game.getHomeTeam().getName();
            String awayTeamName = game.getAwayTeam().getName();
            GameResult result = game.getResult();

            TeamStats homeStats = teamStatsMap.get(homeTeamName);
            TeamStats awayStats = teamStatsMap.get(awayTeamName);

            if (homeStats != null && awayStats != null) {
                // 경기 결과에 따라 각 팀의 승/무/패 기록
                switch (result) {
                    case HOME_WIN:
                        homeStats.addWin();
                        awayStats.addLoss();
                        break;
                    case AWAY_WIN:
                        homeStats.addLoss();
                        awayStats.addWin();
                        break;
                    case DRAW:
                        homeStats.addDraw();
                        awayStats.addDraw();
                        break;
                    default:
                        // CANCELLED 등은 무시
                        break;
                }
            }
        }

        // 5. 승률 기준으로 정렬하여 순위 계산
        List<TeamStats> rankedStats = teamStatsMap.values().stream()
                .sorted((a, b) -> {
                    // 1차 정렬: 승률 기준 내림차순
                    int winRateCompare = Double.compare(b.getWinRate(), a.getWinRate());
                    if (winRateCompare != 0) return winRateCompare;
                    
                    // 2차 정렬: 승수 기준 내림차순
                    int winsCompare = Integer.compare(b.getWins(), a.getWins());
                    if (winsCompare != 0) return winsCompare;
                    
                    // 3차 정렬: 경기수 기준 내림차순
                    return Integer.compare(b.getGames(), a.getGames());
                })
                .collect(Collectors.toList());

        // 6. 경기차 계산 (1위팀 기준)
        if (!rankedStats.isEmpty()) {
            calculateGamesBehind(rankedStats);
        }

        // 7. 연승/연패 계산
        calculateStreaks(finishedGames, teamStatsMap);

        log.info("{} 구단 순위 계산 완료 - {}개 팀", period, rankedStats.size());
        rankedStats.forEach(stats -> log.debug("{}", stats));

        return rankedStats;
    }

    /**
     * 1위팀 기준 경기차 계산
     * 공식: (1위팀 승수 - 응원팀 승수) * 0.5 + (응원팀 패수 - 1위팀 패수) * 0.5
     */
    private void calculateGamesBehind(List<TeamStats> rankedStats) {
        if (rankedStats.isEmpty()) return;

        TeamStats firstPlace = rankedStats.get(0);
        int firstPlaceWins = firstPlace.getWins();
        int firstPlaceLosses = firstPlace.getLosses();

        for (TeamStats stats : rankedStats) {
            if (stats == firstPlace) {
                stats.setGameBehind(0.0); // 1위는 경기차 0
            } else {
                // 경기차 = (1위승수 - 해당팀승수) * 0.5 + (해당팀패수 - 1위패수) * 0.5
                double gameBehind = (firstPlaceWins - stats.getWins()) * 0.5 + (stats.getLosses() - firstPlaceLosses) * 0.5;
                stats.setGameBehind(Math.max(0.0, gameBehind)); // 음수 방지
            }
        }
    }

    /**
     * 각 팀의 연승/연패 계산
     * 날짜순으로 정렬된 경기를 순회하면서 최근 연승/연패 계산
     */
    private void calculateStreaks(List<Game> games, Map<String, TeamStats> teamStatsMap) {
        // 날짜순으로 정렬
        List<Game> sortedGames = games.stream()
                .sorted(Comparator.comparing(Game::getDateTime))
                .collect(Collectors.toList());

        // 팀별로 최근 경기 결과 추적
        Map<String, Integer> teamStreaks = new HashMap<>();
        
        for (Game game : sortedGames) {
            String homeTeamName = game.getHomeTeam().getName();
            String awayTeamName = game.getAwayTeam().getName();
            GameResult result = game.getResult();

            // 무승부가 아닌 경우에만 연승/연패 계산
            if (result != GameResult.DRAW && result != GameResult.CANCELLED) {
                boolean homeWin = (result == GameResult.HOME_WIN);
                boolean awayWin = (result == GameResult.AWAY_WIN);
                
                updateTeamStreak(teamStreaks, homeTeamName, homeWin);
                updateTeamStreak(teamStreaks, awayTeamName, awayWin);
            }
        }

        // 계산된 연승/연패를 TeamStats에 반영
        for (Map.Entry<String, Integer> entry : teamStreaks.entrySet()) {
            TeamStats stats = teamStatsMap.get(entry.getKey());
            if (stats != null) {
                stats.setStreak(entry.getValue());
            }
        }
    }

    /**
     * 개별 팀의 연승/연패 업데이트
     * @param teamStreaks 팀별 연승/연패 맵
     * @param teamName 팀명
     * @param isWin 승리 여부
     */
    private void updateTeamStreak(Map<String, Integer> teamStreaks, String teamName, boolean isWin) {
        int currentStreak = teamStreaks.getOrDefault(teamName, 0);
        
        if (isWin) {
            // 승리한 경우
            if (currentStreak >= 0) {
                teamStreaks.put(teamName, currentStreak + 1); // 연승 증가
            } else {
                teamStreaks.put(teamName, 1); // 연패 끝나고 연승 시작
            }
        } else {
            // 패배한 경우
            if (currentStreak <= 0) {
                teamStreaks.put(teamName, currentStreak - 1); // 연패 증가
            } else {
                teamStreaks.put(teamName, -1); // 연승 끝나고 연패 시작
            }
        }
    }

    /**
     * 2025년 현재까지의 순위를 계산하고 Redis에 영구 저장
     */
    @Transactional(readOnly = true)
    public void cacheCurrentRanking() {
        try {
            log.info("2025년 현재까지 순위 계산 및 캐시 시작");
            
            List<TeamStats> rankings = calculateTeamRanking();
            String rankingJson = objectMapper.writeValueAsString(rankings);
            
            // 만료시간 없이 영구 저장
            redisTemplate.opsForValue().set(CURRENT_RANKING_KEY, rankingJson);
            
            log.info("✅ 2025년 현재까지 순위 캐시 완료 - {}개 팀", rankings.size());
            
        } catch (JsonProcessingException e) {
            log.error("순위 데이터 JSON 직렬화 실패", e);
        } catch (Exception e) {
            log.error("현재 순위 캐시 실패", e);
        }
    }
}
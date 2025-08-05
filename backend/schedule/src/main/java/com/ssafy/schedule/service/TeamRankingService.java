package com.ssafy.schedule.service;

import com.ssafy.schedule.common.GameResult;
import com.ssafy.schedule.common.GameStatus;
import com.ssafy.schedule.entity.Game;
import com.ssafy.schedule.entity.Team;
import com.ssafy.schedule.repository.GameRepository;
import com.ssafy.schedule.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 구단 순위 계산 서비스
 * - 올해 완료된 경기를 바탕으로 구단별 승률, 승무패, 연승/연패, 경기차 계산
 * - Redis에 순위 데이터 저장
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TeamRankingService {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;

    /**
     * 구단 순위 통계 클래스
     */
    public static class TeamStats {
        private final String teamName;
        private int games = 0;          // 총 경기수
        private int wins = 0;           // 승
        private int draws = 0;          // 무
        private int losses = 0;         // 패
        private double winRate = 0.0;   // 승률
        private int streak = 0;         // 연승(+)/연패(-) (0이면 연승/연패 없음)
        private double gameBehind = 0.0; // 경기차

        public TeamStats(String teamName) {
            this.teamName = teamName;
        }

        // Getters
        public String getTeamName() { return teamName; }
        public int getGames() { return games; }
        public int getWins() { return wins; }
        public int getDraws() { return draws; }
        public int getLosses() { return losses; }
        public double getWinRate() { return winRate; }
        public int getStreak() { return streak; }
        public double getGameBehind() { return gameBehind; }

        // 경기 결과 추가
        public void addGame(GameResult result) {
            games++;
            switch (result) {
                case HOME_WIN:
                case AWAY_WIN:
                    wins++;
                    break;
                case DRAW:
                    draws++;
                    break;
                default:
                    losses++;
                    break;
            }
            calculateWinRate();
        }

        // 승률 계산 (무승부 제외)
        private void calculateWinRate() {
            int totalDecisiveGames = wins + losses;
            if (totalDecisiveGames > 0) {
                winRate = (double) wins / totalDecisiveGames;
            } else {
                winRate = 0.0;
            }
        }

        // 연승/연패 설정
        public void setStreak(int streak) {
            this.streak = streak;
        }

        // 경기차 설정
        public void setGameBehind(double gameBehind) {
            this.gameBehind = gameBehind;
        }

        @Override
        public String toString() {
            return String.format("%s: %d경기 %d승%d무%d패 (승률 %.3f) 연승/연패: %d 경기차: %.1f", 
                    teamName, games, wins, draws, losses, winRate, streak, gameBehind);
        }
    }

    /**
     * 특정 연도의 구단 순위를 계산
     * 
     * @param year 계산할 연도
     * @return 순위별로 정렬된 구단 통계 리스트
     */
    @Transactional(readOnly = true)
    public List<TeamStats> calculateTeamRanking(int year) {
        log.info("{}년 구단 순위 계산 시작", year);

        // 1. 해당 연도 완료된 경기 조회
        List<Game> finishedGames = gameRepository.findByStatusAndYear(GameStatus.FINISHED, year);
        log.info("{}년 완료된 경기 수: {}경기", year, finishedGames.size());

        if (finishedGames.isEmpty()) {
            log.warn("{}년 완료된 경기가 없습니다.", year);
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
                // 홈팀 관점에서 결과 추가
                homeStats.addGame(result);
                
                // 원정팀 관점에서 결과 추가 (결과 반전)
                GameResult awayResult = getOppositeResult(result);
                awayStats.addGame(awayResult);
            }
        }

        // 5. 연승/연패 계산
        calculateStreaks(finishedGames, teamStatsMap);

        // 6. 순위 정렬 (승률 기준 내림차순)
        List<TeamStats> rankedStats = teamStatsMap.values().stream()
                .sorted((a, b) -> {
                    // 승률로 1차 정렬
                    int winRateCompare = Double.compare(b.getWinRate(), a.getWinRate());
                    if (winRateCompare != 0) return winRateCompare;
                    
                    // 승률이 같으면 승수로 2차 정렬
                    int winsCompare = Integer.compare(b.getWins(), a.getWins());
                    if (winsCompare != 0) return winsCompare;
                    
                    // 승수도 같으면 경기수로 3차 정렬
                    return Integer.compare(b.getGames(), a.getGames());
                })
                .collect(Collectors.toList());

        // 7. 경기차 계산 (1위팀 기준)
        if (!rankedStats.isEmpty()) {
            calculateGamesBehind(rankedStats);
        }

        log.info("{}년 구단 순위 계산 완료 - {}개 팀", year, rankedStats.size());
        rankedStats.forEach(stats -> log.debug("{}", stats));

        return rankedStats;
    }

    /**
     * 경기 결과의 반대 결과 반환 (원정팀 관점)
     */
    private GameResult getOppositeResult(GameResult result) {
        switch (result) {
            case HOME_WIN:
                return GameResult.AWAY_WIN;
            case AWAY_WIN:
                return GameResult.HOME_WIN;
            case DRAW:
                return GameResult.DRAW;
            default:
                return result; // CANCELLED 등은 그대로
        }
    }

    /**
     * 각 팀의 연승/연패 계산
     */
    private void calculateStreaks(List<Game> games, Map<String, TeamStats> teamStatsMap) {
        // 팀별로 최근 경기 결과 추적
        Map<String, Integer> teamStreaks = new HashMap<>();
        
        // 날짜순으로 정렬된 경기를 순회하면서 연승/연패 계산
        for (Game game : games) {
            String homeTeamName = game.getHomeTeam().getName();
            String awayTeamName = game.getAwayTeam().getName();
            GameResult result = game.getResult();

            updateTeamStreak(teamStreaks, homeTeamName, result == GameResult.HOME_WIN);
            updateTeamStreak(teamStreaks, awayTeamName, result == GameResult.AWAY_WIN);
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
     * 1위팀 기준 경기차 계산
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
                // 경기차 = ((1위승수 - 해당팀승수) + (해당팀패수 - 1위패수)) / 2
                double gameBehind = ((firstPlaceWins - stats.getWins()) + (stats.getLosses() - firstPlaceLosses)) / 2.0;
                stats.setGameBehind(Math.max(0.0, gameBehind)); // 음수 방지
            }
        }
    }

    /**
     * 현재 연도 구단 순위 계산
     */
    public List<TeamStats> calculateCurrentYearRanking() {
        int currentYear = LocalDate.now().getYear();
        return calculateTeamRanking(currentYear);
    }
}
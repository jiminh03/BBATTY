package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.domain.statistics.dto.response.UserBasicStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserDetailedStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserStreakStatsResponse;
import com.ssafy.schedule.domain.statistics.repository.StatisticsRedisRepository;
import com.ssafy.schedule.global.constants.GameResult;
import com.ssafy.schedule.global.entity.Game;
import com.ssafy.schedule.global.entity.Team;
import com.ssafy.schedule.global.repository.GameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StatisticsServiceImpl 테스트")
class StatisticsServiceImplTest {

    @Mock
    private StatisticsRedisRepository statisticsRedisRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private Team createMockTeam(Long id, String name) {
        Team team = mock(Team.class);
        given(team.getId()).willReturn(id);
        given(team.getName()).willReturn(name);
        return team;
    }

    private Game createMockGame(Long gameId, Team homeTeam, Team awayTeam, GameResult result, LocalDateTime dateTime) {
        Game game = mock(Game.class);
        given(game.getId()).willReturn(gameId);
        given(game.getHomeTeam()).willReturn(homeTeam);
        given(game.getAwayTeam()).willReturn(awayTeam);
        given(game.getResult()).willReturn(result);
        given(game.getDateTime()).willReturn(dateTime);
        return game;
    }

    @Test
    @DisplayName("사용자 기본 통계 계산 - 캐시 없을 때")
    void calculateUserBasicStats_NoCacheFound_Success() {
        // given
        Long userId = 1L;
        String season = "2025";
        Long teamId = 10L;
        
        given(statisticsRedisRepository.getUserWinRate(userId, season)).willReturn(null);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(anyString(), anyLong(), anyLong()))
                .willReturn(Set.of(
                    "{\"gameId\":1,\"homeTeam\":\"두산\",\"awayTeam\":\"LG\",\"dateTime\":\"2025-01-01T14:00:00\",\"stadium\":\"잠실야구장\"}",
                    "{\"gameId\":2,\"homeTeam\":\"KIA\",\"awayTeam\":\"두산\",\"dateTime\":\"2025-01-02T14:00:00\",\"stadium\":\"광주\"}"
                ));

        Team homeTeam = createMockTeam(10L, "두산");
        Team awayTeam = createMockTeam(20L, "LG");
        Game game1 = createMockGame(1L, homeTeam, awayTeam, GameResult.HOME_WIN, LocalDateTime.of(2025, 1, 1, 14, 0));
        
        Team kiaTeam = createMockTeam(30L, "KIA");
        Game game2 = createMockGame(2L, kiaTeam, homeTeam, GameResult.AWAY_WIN, LocalDateTime.of(2025, 1, 2, 14, 0));
        
        given(gameRepository.findById(1L)).willReturn(Optional.of(game1));
        given(gameRepository.findById(2L)).willReturn(Optional.of(game2));

        // when
        UserBasicStatsResponse result = statisticsService.calculateUserBasicStats(userId, season, teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getSeason()).isEqualTo(season);
        assertThat(result.getTotalGames()).isEqualTo(2);
        assertThat(result.getWins()).isEqualTo(2);
        assertThat(result.getDraws()).isEqualTo(0);
        assertThat(result.getLosses()).isEqualTo(0);
        assertThat(result.getWinRate()).isEqualTo("1.000");
        
        verify(statisticsRedisRepository).saveUserWinRate(userId, season, result);
    }

    @Test
    @DisplayName("사용자 기본 통계 계산 - 캐시에서 조회 성공")
    void calculateUserBasicStats_CacheHit_Success() {
        // given
        Long userId = 1L;
        String season = "2025";
        Long teamId = 10L;
        
        UserBasicStatsResponse cachedResponse = UserBasicStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(5)
                .wins(3)
                .draws(1)
                .losses(1)
                .winRate("0.750")
                .build();
        
        given(statisticsRedisRepository.getUserWinRate(userId, season)).willReturn(cachedResponse);

        // when
        UserBasicStatsResponse result = statisticsService.calculateUserBasicStats(userId, season, teamId);

        // then
        assertThat(result).isEqualTo(cachedResponse);
        verify(statisticsRedisRepository, never()).saveUserWinRate(any(), any(), any());
        verify(redisTemplate, never()).opsForZSet();
    }

    @Test
    @DisplayName("사용자 기본 통계 계산 - 데이터 없을 때")
    void calculateUserBasicStats_NoData_ReturnsEmptyStats() {
        // given
        Long userId = 1L;
        String season = "2025";
        Long teamId = 10L;
        
        given(statisticsRedisRepository.getUserWinRate(userId, season)).willReturn(null);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(anyString(), anyLong(), anyLong())).willReturn(Set.of());

        // when
        UserBasicStatsResponse result = statisticsService.calculateUserBasicStats(userId, season, teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getSeason()).isEqualTo(season);
        assertThat(result.getTotalGames()).isEqualTo(0);
        assertThat(result.getWins()).isEqualTo(0);
        assertThat(result.getDraws()).isEqualTo(0);
        assertThat(result.getLosses()).isEqualTo(0);
        assertThat(result.getWinRate()).isEqualTo("0.000");
    }

    @Test
    @DisplayName("사용자 상세 통계 계산 - 캐시 없을 때")
    void calculateUserDetailedStats_NoCacheFound_Success() {
        // given
        Long userId = 1L;
        String season = "2025";
        Long teamId = 10L;
        
        given(statisticsRedisRepository.getUserDetailedStats(userId, season)).willReturn(null);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(anyString(), anyLong(), anyLong()))
                .willReturn(Set.of(
                    "{\"gameId\":1,\"homeTeam\":\"두산\",\"awayTeam\":\"LG\",\"dateTime\":\"2025-01-01T14:00:00\",\"stadium\":\"잠실야구장\"}"
                ));

        Team homeTeam = createMockTeam(10L, "두산");
        Team awayTeam = createMockTeam(20L, "LG");
        Game game = createMockGame(1L, homeTeam, awayTeam, GameResult.HOME_WIN, LocalDateTime.of(2025, 1, 1, 14, 0));
        
        given(gameRepository.findById(1L)).willReturn(Optional.of(game));

        // when
        UserDetailedStatsResponse result = statisticsService.calculateUserDetailedStats(userId, season, teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getSeason()).isEqualTo(season);
        assertThat(result.getTotalGames()).isEqualTo(1);
        assertThat(result.getWins()).isEqualTo(1);
        assertThat(result.getStadiumStats()).isNotEmpty();
        assertThat(result.getOpponentStats()).isNotEmpty();
        assertThat(result.getDayOfWeekStats()).isNotEmpty();
        assertThat(result.getHomeStats()).isNotNull();
        assertThat(result.getAwayStats()).isNotNull();
        
        verify(statisticsRedisRepository).saveUserDetailedStats(userId, season, result);
    }

    @Test
    @DisplayName("사용자 상세 통계 계산 - 캐시에서 조회 성공")
    void calculateUserDetailedStats_CacheHit_Success() {
        // given
        Long userId = 1L;
        String season = "2025";
        Long teamId = 10L;
        
        UserDetailedStatsResponse cachedResponse = UserDetailedStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(3)
                .wins(2)
                .draws(0)
                .losses(1)
                .winRate("0.667")
                .stadiumStats(new HashMap<>())
                .opponentStats(new HashMap<>())
                .dayOfWeekStats(new HashMap<>())
                .homeStats(UserDetailedStatsResponse.CategoryStats.empty())
                .awayStats(UserDetailedStatsResponse.CategoryStats.empty())
                .build();
        
        given(statisticsRedisRepository.getUserDetailedStats(userId, season)).willReturn(cachedResponse);

        // when
        UserDetailedStatsResponse result = statisticsService.calculateUserDetailedStats(userId, season, teamId);

        // then
        assertThat(result).isEqualTo(cachedResponse);
        verify(statisticsRedisRepository, never()).saveUserDetailedStats(any(), any(), any());
    }

    @Test
    @DisplayName("사용자 연승 통계 계산 - 캐시 없을 때")
    void calculateUserStreakStats_NoCacheFound_Success() {
        // given
        Long userId = 1L;
        Long teamId = 10L;
        
        given(statisticsRedisRepository.getUserStreakStats(userId)).willReturn(null);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(anyString(), anyLong(), anyLong()))
                .willReturn(Set.of(
                    "{\"gameId\":1,\"homeTeam\":\"두산\",\"awayTeam\":\"LG\",\"dateTime\":\"2025-01-01T14:00:00\",\"stadium\":\"잠실야구장\"}",
                    "{\"gameId\":2,\"homeTeam\":\"두산\",\"awayTeam\":\"KIA\",\"dateTime\":\"2025-01-02T14:00:00\",\"stadium\":\"잠실야구장\"}"
                ));

        Team homeTeam = createMockTeam(10L, "두산");
        Team awayTeam = createMockTeam(20L, "LG");
        Team kiaTeam = createMockTeam(30L, "KIA");
        
        Game game1 = createMockGame(1L, homeTeam, awayTeam, GameResult.HOME_WIN, LocalDateTime.of(2025, 1, 1, 14, 0));
        Game game2 = createMockGame(2L, homeTeam, kiaTeam, GameResult.HOME_WIN, LocalDateTime.of(2025, 1, 2, 14, 0));
        
        given(gameRepository.findById(1L)).willReturn(Optional.of(game1));
        given(gameRepository.findById(2L)).willReturn(Optional.of(game2));

        // when
        UserStreakStatsResponse result = statisticsService.calculateUserStreakStats(userId, teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCurrentSeason()).isEqualTo("2025");
        assertThat(result.getCurrentWinStreak()).isEqualTo(2);
        assertThat(result.getMaxWinStreakAll()).isEqualTo(2);
        assertThat(result.getMaxWinStreakCurrentSeason()).isEqualTo(2);
        
        verify(statisticsRedisRepository).saveUserStreakStats(userId, result);
    }

    @Test
    @DisplayName("사용자 연승 통계 계산 - 캐시에서 조회 성공")
    void calculateUserStreakStats_CacheHit_Success() {
        // given
        Long userId = 1L;
        Long teamId = 10L;
        
        UserStreakStatsResponse cachedResponse = UserStreakStatsResponse.builder()
                .userId(userId)
                .currentSeason("2025")
                .currentWinStreak(5)
                .maxWinStreakAll(8)
                .maxWinStreakCurrentSeason(5)
                .maxWinStreakBySeason(Map.of("2024", 3, "2025", 5))
                .build();
        
        given(statisticsRedisRepository.getUserStreakStats(userId)).willReturn(cachedResponse);

        // when
        UserStreakStatsResponse result = statisticsService.calculateUserStreakStats(userId, teamId);

        // then
        assertThat(result).isEqualTo(cachedResponse);
        verify(statisticsRedisRepository, never()).saveUserStreakStats(any(), any());
    }

    @Test
    @DisplayName("사용자 통계 재계산 - 캐시 삭제")
    void recalculateUserStats_Success() {
        // given
        Long userId = 1L;

        // when
        statisticsService.recalculateUserStats(userId);

        // then
        verify(statisticsRedisRepository).clearCurrentSeasonAndTotalStats(eq(userId), eq("2025"));
    }

    @Test
    @DisplayName("승률 계산 - 승무패 다양한 경우")
    void calculateWinRate_VariousCases() {
        // given & when & then
        assertThat(UserBasicStatsResponse.calculateWinRate(0, 0)).isEqualTo("0.000");
        assertThat(UserBasicStatsResponse.calculateWinRate(1, 0)).isEqualTo("1.000");
        assertThat(UserBasicStatsResponse.calculateWinRate(0, 1)).isEqualTo("0.000");
        assertThat(UserBasicStatsResponse.calculateWinRate(3, 1)).isEqualTo("0.750");
        assertThat(UserBasicStatsResponse.calculateWinRate(1, 2)).isEqualTo("0.333");
        assertThat(UserBasicStatsResponse.calculateWinRate(2, 2)).isEqualTo("0.500");
    }

    @Test
    @DisplayName("연승 계산 - 패배로 연승 중단")
    void calculateStreakStats_WinStreakBrokenByLoss() {
        // given
        Long userId = 1L;
        Long teamId = 10L;
        
        given(statisticsRedisRepository.getUserStreakStats(userId)).willReturn(null);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        
        // LinkedHashSet을 사용하여 순서 보장 (최신순으로 Redis에서 반환)
        Set<Object> orderedSet = new java.util.LinkedHashSet<>();
        orderedSet.add("{\"gameId\":3,\"homeTeam\":\"LG\",\"awayTeam\":\"두산\",\"dateTime\":\"2025-01-03T14:00:00\",\"stadium\":\"잠실야구장\"}");
        orderedSet.add("{\"gameId\":2,\"homeTeam\":\"두산\",\"awayTeam\":\"KIA\",\"dateTime\":\"2025-01-02T14:00:00\",\"stadium\":\"잠실야구장\"}");
        orderedSet.add("{\"gameId\":1,\"homeTeam\":\"두산\",\"awayTeam\":\"LG\",\"dateTime\":\"2025-01-01T14:00:00\",\"stadium\":\"잠실야구장\"}");
        
        given(zSetOperations.reverseRange(anyString(), anyLong(), anyLong())).willReturn(orderedSet);

        Team homeTeam = createMockTeam(10L, "두산");
        Team awayTeam = createMockTeam(20L, "LG");
        Team kiaTeam = createMockTeam(30L, "KIA");
        
        Game game1 = createMockGame(1L, homeTeam, awayTeam, GameResult.HOME_WIN, LocalDateTime.of(2025, 1, 1, 14, 0));
        Game game2 = createMockGame(2L, homeTeam, kiaTeam, GameResult.HOME_WIN, LocalDateTime.of(2025, 1, 2, 14, 0));
        Game game3 = createMockGame(3L, awayTeam, homeTeam, GameResult.HOME_WIN, LocalDateTime.of(2025, 1, 3, 14, 0)); // 두산 패배
        
        given(gameRepository.findById(1L)).willReturn(Optional.of(game1));
        given(gameRepository.findById(2L)).willReturn(Optional.of(game2));
        given(gameRepository.findById(3L)).willReturn(Optional.of(game3));

        // when
        UserStreakStatsResponse result = statisticsService.calculateUserStreakStats(userId, teamId);

        // then
        assertThat(result.getCurrentWinStreak()).isEqualTo(0); // 마지막 경기(game3)에서 패배
        assertThat(result.getMaxWinStreakAll()).isEqualTo(2); // 최대 연승은 2경기
    }

    @Test
    @DisplayName("무승부는 연승에 영향 없음")
    void calculateStreakStats_DrawDoesNotAffectStreak() {
        // given
        Long userId = 1L;
        Long teamId = 10L;
        
        given(statisticsRedisRepository.getUserStreakStats(userId)).willReturn(null);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(anyString(), anyLong(), anyLong()))
                .willReturn(Set.of(
                    "{\"gameId\":1,\"homeTeam\":\"두산\",\"awayTeam\":\"LG\",\"dateTime\":\"2025-01-01T14:00:00\",\"stadium\":\"잠실야구장\"}",
                    "{\"gameId\":2,\"homeTeam\":\"두산\",\"awayTeam\":\"KIA\",\"dateTime\":\"2025-01-02T14:00:00\",\"stadium\":\"잠실야구장\"}"
                ));

        Team homeTeam = createMockTeam(10L, "두산");
        Team awayTeam = createMockTeam(20L, "LG");
        Team kiaTeam = createMockTeam(30L, "KIA");
        
        Game game1 = createMockGame(1L, homeTeam, awayTeam, GameResult.HOME_WIN, LocalDateTime.of(2025, 1, 1, 14, 0));
        Game game2 = createMockGame(2L, homeTeam, kiaTeam, GameResult.DRAW, LocalDateTime.of(2025, 1, 2, 14, 0)); // 무승부
        
        given(gameRepository.findById(1L)).willReturn(Optional.of(game1));
        given(gameRepository.findById(2L)).willReturn(Optional.of(game2));

        // when
        UserStreakStatsResponse result = statisticsService.calculateUserStreakStats(userId, teamId);

        // then
        assertThat(result.getCurrentWinStreak()).isEqualTo(1); // 무승부는 연승을 끊지 않음
        assertThat(result.getMaxWinStreakAll()).isEqualTo(1);
    }
}
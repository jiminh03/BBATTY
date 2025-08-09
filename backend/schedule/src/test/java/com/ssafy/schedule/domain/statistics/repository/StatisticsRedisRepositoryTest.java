package com.ssafy.schedule.domain.statistics.repository;

import com.ssafy.schedule.domain.statistics.dto.response.UserBasicStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserDetailedStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserStreakStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("StatisticsRedisRepository 테스트")
class StatisticsRedisRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private StatisticsRedisRepository statisticsRedisRepository;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("사용자 승률 통계 저장 성공")
    void saveUserWinRate_Success() {
        // given
        Long userId = 1L;
        String season = "2025";
        UserBasicStatsResponse winRateStats = UserBasicStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(10)
                .wins(7)
                .draws(1)
                .losses(2)
                .winRate("0.778")
                .build();

        // when
        statisticsRedisRepository.saveUserWinRate(userId, season, winRateStats);

        // then
        String expectedKey = "stats:user:winrate:" + userId + ":" + season;
        verify(valueOperations).set(expectedKey, winRateStats, Duration.ofHours(24));
    }

    @Test
    @DisplayName("사용자 승률 통계 조회 성공")
    void getUserWinRate_Success() {
        // given
        Long userId = 1L;
        String season = "2025";
        UserBasicStatsResponse expectedStats = UserBasicStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(5)
                .wins(3)
                .draws(1)
                .losses(1)
                .winRate("0.750")
                .build();

        String expectedKey = "stats:user:winrate:" + userId + ":" + season;
        given(valueOperations.get(expectedKey)).willReturn(expectedStats);

        // when
        Object result = statisticsRedisRepository.getUserWinRate(userId, season);

        // then
        assertThat(result).isEqualTo(expectedStats);
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("사용자 승률 통계 조회 - 데이터 없음")
    void getUserWinRate_NotFound() {
        // given
        Long userId = 999L;
        String season = "2025";
        String expectedKey = "stats:user:winrate:" + userId + ":" + season;
        given(valueOperations.get(expectedKey)).willReturn(null);

        // when
        Object result = statisticsRedisRepository.getUserWinRate(userId, season);

        // then
        assertThat(result).isNull();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("사용자 연승 통계 저장 성공")
    void saveUserStreakStats_Success() {
        // given
        Long userId = 1L;
        UserStreakStatsResponse streakStats = UserStreakStatsResponse.builder()
                .userId(userId)
                .currentSeason("2025")
                .currentWinStreak(3)
                .maxWinStreakAll(8)
                .maxWinStreakCurrentSeason(5)
                .maxWinStreakBySeason(Map.of("2024", 8, "2025", 5))
                .build();

        // when
        statisticsRedisRepository.saveUserStreakStats(userId, streakStats);

        // then
        String expectedKey = "stats:user:streak:" + userId;
        verify(valueOperations).set(expectedKey, streakStats, Duration.ofDays(30));
    }

    @Test
    @DisplayName("사용자 연승 통계 조회 성공")
    void getUserStreakStats_Success() {
        // given
        Long userId = 1L;
        UserStreakStatsResponse expectedStats = UserStreakStatsResponse.builder()
                .userId(userId)
                .currentSeason("2025")
                .currentWinStreak(2)
                .maxWinStreakAll(10)
                .maxWinStreakCurrentSeason(6)
                .maxWinStreakBySeason(Map.of("2024", 10, "2025", 6))
                .build();

        String expectedKey = "stats:user:streak:" + userId;
        given(valueOperations.get(expectedKey)).willReturn(expectedStats);

        // when
        Object result = statisticsRedisRepository.getUserStreakStats(userId);

        // then
        assertThat(result).isEqualTo(expectedStats);
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("사용자 상세 통계 저장 성공")
    void saveUserDetailedStats_Success() {
        // given
        Long userId = 1L;
        String season = "2025";
        UserDetailedStatsResponse detailedStats = UserDetailedStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(15)
                .wins(10)
                .draws(2)
                .losses(3)
                .winRate("0.769")
                .stadiumStats(new HashMap<>())
                .opponentStats(new HashMap<>())
                .dayOfWeekStats(new HashMap<>())
                .homeStats(UserDetailedStatsResponse.CategoryStats.builder()
                        .games(8).wins(6).draws(1).losses(1).winRate("0.857").build())
                .awayStats(UserDetailedStatsResponse.CategoryStats.builder()
                        .games(7).wins(4).draws(1).losses(2).winRate("0.667").build())
                .build();

        // when
        statisticsRedisRepository.saveUserDetailedStats(userId, season, detailedStats);

        // then
        String expectedKey = "stats:user:detailed:" + userId + ":" + season;
        verify(valueOperations).set(expectedKey, detailedStats, Duration.ofHours(12));
    }

    @Test
    @DisplayName("사용자 상세 통계 조회 성공")
    void getUserDetailedStats_Success() {
        // given
        Long userId = 1L;
        String season = "2025";
        UserDetailedStatsResponse expectedStats = UserDetailedStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(20)
                .wins(12)
                .draws(3)
                .losses(5)
                .winRate("0.706")
                .stadiumStats(new HashMap<>())
                .opponentStats(new HashMap<>())
                .dayOfWeekStats(new HashMap<>())
                .homeStats(UserDetailedStatsResponse.CategoryStats.empty())
                .awayStats(UserDetailedStatsResponse.CategoryStats.empty())
                .build();

        String expectedKey = "stats:user:detailed:" + userId + ":" + season;
        given(valueOperations.get(expectedKey)).willReturn(expectedStats);

        // when
        Object result = statisticsRedisRepository.getUserDetailedStats(userId, season);

        // then
        assertThat(result).isEqualTo(expectedStats);
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("사용자 현재 시즌 및 통산 통계 캐시 삭제")
    void clearCurrentSeasonAndTotalStats_Success() {
        // given
        Long userId = 1L;
        String currentSeason = "2025";

        // when
        statisticsRedisRepository.clearCurrentSeasonAndTotalStats(userId, currentSeason);

        // then
        String currentSeasonWinRateKey = "stats:user:winrate:" + userId + ":" + currentSeason;
        String currentSeasonDetailedKey = "stats:user:detailed:" + userId + ":" + currentSeason;
        String totalWinRateKey = "stats:user:winrate:" + userId + ":total";
        String totalDetailedKey = "stats:user:detailed:" + userId + ":total";
        String streakStatsKey = "stats:user:streak:" + userId;

        verify(redisTemplate).delete(currentSeasonWinRateKey);
        verify(redisTemplate).delete(currentSeasonDetailedKey);
        verify(redisTemplate).delete(totalWinRateKey);
        verify(redisTemplate).delete(totalDetailedKey);
        verify(redisTemplate).delete(streakStatsKey);
    }

    @Test
    @DisplayName("다른 시즌 통계 저장 및 조회 - 통산")
    void saveAndGetUserStats_TotalSeason() {
        // given
        Long userId = 1L;
        String season = "total";
        UserBasicStatsResponse totalStats = UserBasicStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(50)
                .wins(30)
                .draws(5)
                .losses(15)
                .winRate("0.667")
                .build();

        String expectedKey = "stats:user:winrate:" + userId + ":" + season;

        // when
        statisticsRedisRepository.saveUserWinRate(userId, season, totalStats);

        // then
        verify(valueOperations).set(expectedKey, totalStats, Duration.ofHours(24));
    }

    @Test
    @DisplayName("이전 시즌 통계 저장 및 조회")
    void saveAndGetUserStats_PreviousSeason() {
        // given
        Long userId = 1L;
        String season = "2024";
        UserDetailedStatsResponse previousSeasonStats = UserDetailedStatsResponse.builder()
                .userId(userId)
                .season(season)
                .totalGames(30)
                .wins(18)
                .draws(4)
                .losses(8)
                .winRate("0.692")
                .stadiumStats(Map.of("잠실야구장", 
                        UserDetailedStatsResponse.CategoryStats.builder()
                                .games(15).wins(10).draws(2).losses(3).winRate("0.769").build()))
                .opponentStats(new HashMap<>())
                .dayOfWeekStats(new HashMap<>())
                .homeStats(UserDetailedStatsResponse.CategoryStats.empty())
                .awayStats(UserDetailedStatsResponse.CategoryStats.empty())
                .build();

        String expectedKey = "stats:user:detailed:" + userId + ":" + season;

        // when
        statisticsRedisRepository.saveUserDetailedStats(userId, season, previousSeasonStats);

        // then
        verify(valueOperations).set(expectedKey, previousSeasonStats, Duration.ofHours(12));
    }
}
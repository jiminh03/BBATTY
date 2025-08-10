package com.ssafy.schedule.domain.statistics.service;

import com.ssafy.schedule.domain.statistics.dto.internal.AttendanceRecord;
import com.ssafy.schedule.domain.statistics.dto.response.UserBasicStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserDetailedStatsResponse;
import com.ssafy.schedule.domain.statistics.dto.response.UserStreakStatsResponse;
import com.ssafy.schedule.domain.statistics.repository.StatisticsRedisRepository;
import com.ssafy.schedule.global.constants.Stadium;
import com.ssafy.schedule.global.repository.GameRepository;
import com.ssafy.schedule.global.util.AttendanceRecordParser;
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
import java.time.DayOfWeek;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StatisticsServiceImpl 통합 테스트")
class StatisticsServiceImplTest {

    @Mock
    private StatisticsRedisRepository statisticsRedisRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private AttendanceRecordParser attendanceRecordParser;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

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
        
        verify(statisticsRedisRepository).saveUserWinRate(userId, season, result);
    }

    @Test
    @DisplayName("사용자 기본 통계 계산 - AttendanceRecord로부터 통계 계산")
    void calculateUserBasicStats_WithAttendanceRecords_Success() {
        // given
        Long userId = 1L;
        String season = "2025";
        Long teamId = 10L;
        
        given(statisticsRedisRepository.getUserWinRate(userId, season)).willReturn(null);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(anyString(), anyLong(), anyLong()))
                .willReturn(Set.of("record1", "record2", "record3"));

        // Mock AttendanceRecord 생성
        AttendanceRecord winRecord = createMockAttendanceRecord(1L, AttendanceRecord.UserGameResult.WIN, teamId, Stadium.JAMSIL);
        AttendanceRecord drawRecord = createMockAttendanceRecord(2L, AttendanceRecord.UserGameResult.DRAW, teamId, Stadium.SUWON);  
        AttendanceRecord lossRecord = createMockAttendanceRecord(3L, AttendanceRecord.UserGameResult.LOSS, teamId, Stadium.DAEGU);

        given(attendanceRecordParser.parseAttendanceRecord("record1", teamId)).willReturn(winRecord);
        given(attendanceRecordParser.parseAttendanceRecord("record2", teamId)).willReturn(drawRecord);
        given(attendanceRecordParser.parseAttendanceRecord("record3", teamId)).willReturn(lossRecord);

        // when
        UserBasicStatsResponse result = statisticsService.calculateUserBasicStats(userId, season, teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getSeason()).isEqualTo(season);
        assertThat(result.getTotalGames()).isEqualTo(3);
        assertThat(result.getWins()).isEqualTo(1);
        assertThat(result.getDraws()).isEqualTo(1);
        assertThat(result.getLosses()).isEqualTo(1);
        assertThat(result.getWinRate()).isEqualTo("0.500"); // 1승 1패
        
        verify(statisticsRedisRepository).saveUserWinRate(userId, season, result);
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

    // ===========================================
    // Helper Methods
    // ===========================================

    private AttendanceRecord createMockAttendanceRecord(Long gameId, AttendanceRecord.UserGameResult result, Long userTeamId, Stadium stadium) {
        AttendanceRecord record = mock(AttendanceRecord.class);
        given(record.getGameId()).willReturn(gameId);
        given(record.getUserGameResult()).willReturn(result);
        given(record.getUserTeamId()).willReturn(userTeamId);
        given(record.getStadium()).willReturn(stadium.getStadiumName());
        given(record.getGameDateTime()).willReturn(LocalDateTime.now());
        given(record.getDayOfWeek()).willReturn(DayOfWeek.SUNDAY);
        
        // Mock opponent team logic
        Long opponentTeamId = userTeamId == 1L ? 2L : 1L;
        given(record.getOpponentTeamId(userTeamId)).willReturn(opponentTeamId);
        given(record.isHomeGame(userTeamId)).willReturn(true);
        
        return record;
    }
}
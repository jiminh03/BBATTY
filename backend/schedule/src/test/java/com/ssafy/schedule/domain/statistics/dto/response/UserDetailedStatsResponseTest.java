package com.ssafy.schedule.domain.statistics.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserDetailedStatsResponse DTO 테스트")
class UserDetailedStatsResponseTest {

    @Test
    @DisplayName("UserDetailedStatsResponse 빌더 패턴으로 생성")
    void buildUserDetailedStatsResponse_Success() {
        // given
        Map<String, UserDetailedStatsResponse.CategoryStats> stadiumStats = new HashMap<>();
        stadiumStats.put("잠실야구장", UserDetailedStatsResponse.CategoryStats.builder()
                .games(5).wins(3).draws(1).losses(1).winRate("0.750").build());

        Map<String, UserDetailedStatsResponse.CategoryStats> opponentStats = new HashMap<>();
        opponentStats.put("10", UserDetailedStatsResponse.CategoryStats.builder()
                .games(3).wins(2).draws(0).losses(1).winRate("0.667").build());

        Map<String, UserDetailedStatsResponse.CategoryStats> dayOfWeekStats = new HashMap<>();
        dayOfWeekStats.put("SATURDAY", UserDetailedStatsResponse.CategoryStats.builder()
                .games(4).wins(3).draws(0).losses(1).winRate("0.750").build());

        UserDetailedStatsResponse.CategoryStats homeStats = UserDetailedStatsResponse.CategoryStats.builder()
                .games(6).wins(4).draws(1).losses(1).winRate("0.800").build();

        UserDetailedStatsResponse.CategoryStats awayStats = UserDetailedStatsResponse.CategoryStats.builder()
                .games(4).wins(2).draws(1).losses(1).winRate("0.667").build();

        // when
        UserDetailedStatsResponse response = UserDetailedStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(10)
                .wins(6)
                .draws(2)
                .losses(2)
                .winRate("0.750")
                .stadiumStats(stadiumStats)
                .opponentStats(opponentStats)
                .dayOfWeekStats(dayOfWeekStats)
                .homeStats(homeStats)
                .awayStats(awayStats)
                .build();

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getSeason()).isEqualTo("2025");
        assertThat(response.getTotalGames()).isEqualTo(10);
        assertThat(response.getWins()).isEqualTo(6);
        assertThat(response.getDraws()).isEqualTo(2);
        assertThat(response.getLosses()).isEqualTo(2);
        assertThat(response.getWinRate()).isEqualTo("0.750");
        assertThat(response.getStadiumStats()).hasSize(1);
        assertThat(response.getOpponentStats()).hasSize(1);
        assertThat(response.getDayOfWeekStats()).hasSize(1);
        assertThat(response.getHomeStats()).isNotNull();
        assertThat(response.getAwayStats()).isNotNull();
    }

    @Test
    @DisplayName("전체 승률 업데이트")
    void updateWinRate_Success() {
        // given
        UserDetailedStatsResponse response = UserDetailedStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(12)
                .wins(8)
                .draws(2)
                .losses(2)
                .winRate("0.000") // 임시값
                .build();

        // when
        response.updateWinRate();

        // then
        assertThat(response.getWinRate()).isEqualTo("0.800"); // 8승 2패 = 0.800
    }

    @Test
    @DisplayName("전체 승률 업데이트 - 승부 결정 경기가 없는 경우")
    void updateWinRate_NoDecisiveGames_ReturnsZero() {
        // given
        UserDetailedStatsResponse response = UserDetailedStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(5)
                .wins(0)
                .draws(5)
                .losses(0)
                .build();

        // when
        response.updateWinRate();

        // then
        assertThat(response.getWinRate()).isEqualTo("0.000");
    }

    @Test
    @DisplayName("CategoryStats 빌더 패턴으로 생성")
    void buildCategoryStats_Success() {
        // when
        UserDetailedStatsResponse.CategoryStats categoryStats = 
                UserDetailedStatsResponse.CategoryStats.builder()
                        .games(8)
                        .wins(5)
                        .draws(2)
                        .losses(1)
                        .winRate("0.833")
                        .build();

        // then
        assertThat(categoryStats.getGames()).isEqualTo(8);
        assertThat(categoryStats.getWins()).isEqualTo(5);
        assertThat(categoryStats.getDraws()).isEqualTo(2);
        assertThat(categoryStats.getLosses()).isEqualTo(1);
        assertThat(categoryStats.getWinRate()).isEqualTo("0.833");
    }

    @Test
    @DisplayName("CategoryStats 승률 업데이트")
    void categoryStatsUpdateWinRate_Success() {
        // given
        UserDetailedStatsResponse.CategoryStats categoryStats = 
                UserDetailedStatsResponse.CategoryStats.builder()
                        .games(10)
                        .wins(7)
                        .draws(1)
                        .losses(2)
                        .winRate("0.000") // 임시값
                        .build();

        // when
        categoryStats.updateWinRate();

        // then
        assertThat(categoryStats.getWinRate()).isEqualTo("0.778"); // 7승 2패 = 0.778
    }

    @Test
    @DisplayName("CategoryStats 승률 업데이트 - 승부 결정 경기가 없는 경우")
    void categoryStatsUpdateWinRate_NoDecisiveGames_ReturnsZero() {
        // given
        UserDetailedStatsResponse.CategoryStats categoryStats = 
                UserDetailedStatsResponse.CategoryStats.builder()
                        .games(3)
                        .wins(0)
                        .draws(3)
                        .losses(0)
                        .build();

        // when
        categoryStats.updateWinRate();

        // then
        assertThat(categoryStats.getWinRate()).isEqualTo("0.000");
    }

    @Test
    @DisplayName("빈 CategoryStats 생성")
    void createEmptyCategoryStats_Success() {
        // when
        UserDetailedStatsResponse.CategoryStats emptyStats = 
                UserDetailedStatsResponse.CategoryStats.empty();

        // then
        assertThat(emptyStats.getGames()).isEqualTo(0);
        assertThat(emptyStats.getWins()).isEqualTo(0);
        assertThat(emptyStats.getDraws()).isEqualTo(0);
        assertThat(emptyStats.getLosses()).isEqualTo(0);
        assertThat(emptyStats.getWinRate()).isEqualTo("0.000");
    }

    @Test
    @DisplayName("구장별 통계 포함된 상세 통계")
    void detailedStatsWithStadiumStats_Success() {
        // given
        Map<String, UserDetailedStatsResponse.CategoryStats> stadiumStats = new HashMap<>();
        stadiumStats.put("잠실야구장", UserDetailedStatsResponse.CategoryStats.builder()
                .games(8).wins(6).draws(1).losses(1).winRate("0.857").build());
        stadiumStats.put("고척돔", UserDetailedStatsResponse.CategoryStats.builder()
                .games(3).wins(1).draws(1).losses(1).winRate("0.500").build());

        // when
        UserDetailedStatsResponse response = UserDetailedStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(11)
                .wins(7)
                .draws(2)
                .losses(2)
                .stadiumStats(stadiumStats)
                .opponentStats(new HashMap<>())
                .dayOfWeekStats(new HashMap<>())
                .homeStats(UserDetailedStatsResponse.CategoryStats.empty())
                .awayStats(UserDetailedStatsResponse.CategoryStats.empty())
                .build();

        // then
        assertThat(response.getStadiumStats()).hasSize(2);
        assertThat(response.getStadiumStats().get("잠실야구장").getWins()).isEqualTo(6);
        assertThat(response.getStadiumStats().get("고척돔").getWins()).isEqualTo(1);
    }

    @Test
    @DisplayName("상대팀별 통계 포함된 상세 통계")
    void detailedStatsWithOpponentStats_Success() {
        // given
        Map<String, UserDetailedStatsResponse.CategoryStats> opponentStats = new HashMap<>();
        opponentStats.put("10", UserDetailedStatsResponse.CategoryStats.builder()
                .games(4).wins(3).draws(0).losses(1).winRate("0.750").build()); // LG
        opponentStats.put("20", UserDetailedStatsResponse.CategoryStats.builder()
                .games(3).wins(1).draws(1).losses(1).winRate("0.500").build()); // 삼성

        // when
        UserDetailedStatsResponse response = UserDetailedStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(7)
                .wins(4)
                .draws(1)
                .losses(2)
                .stadiumStats(new HashMap<>())
                .opponentStats(opponentStats)
                .dayOfWeekStats(new HashMap<>())
                .homeStats(UserDetailedStatsResponse.CategoryStats.empty())
                .awayStats(UserDetailedStatsResponse.CategoryStats.empty())
                .build();

        // then
        assertThat(response.getOpponentStats()).hasSize(2);
        assertThat(response.getOpponentStats().get("10").getWinRate()).isEqualTo("0.750");
        assertThat(response.getOpponentStats().get("20").getWinRate()).isEqualTo("0.500");
    }

    @Test
    @DisplayName("요일별 통계 포함된 상세 통계")
    void detailedStatsWithDayOfWeekStats_Success() {
        // given
        Map<String, UserDetailedStatsResponse.CategoryStats> dayOfWeekStats = new HashMap<>();
        dayOfWeekStats.put("SATURDAY", UserDetailedStatsResponse.CategoryStats.builder()
                .games(5).wins(4).draws(0).losses(1).winRate("0.800").build());
        dayOfWeekStats.put("SUNDAY", UserDetailedStatsResponse.CategoryStats.builder()
                .games(3).wins(2).draws(1).losses(0).winRate("1.000").build());

        // when
        UserDetailedStatsResponse response = UserDetailedStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(8)
                .wins(6)
                .draws(1)
                .losses(1)
                .stadiumStats(new HashMap<>())
                .opponentStats(new HashMap<>())
                .dayOfWeekStats(dayOfWeekStats)
                .homeStats(UserDetailedStatsResponse.CategoryStats.empty())
                .awayStats(UserDetailedStatsResponse.CategoryStats.empty())
                .build();

        // then
        assertThat(response.getDayOfWeekStats()).hasSize(2);
        assertThat(response.getDayOfWeekStats().get("SATURDAY").getWinRate()).isEqualTo("0.800");
        assertThat(response.getDayOfWeekStats().get("SUNDAY").getWinRate()).isEqualTo("1.000");
    }

    @Test
    @DisplayName("홈/원정 통계가 포함된 상세 통계")
    void detailedStatsWithHomeAwayStats_Success() {
        // given
        UserDetailedStatsResponse.CategoryStats homeStats = UserDetailedStatsResponse.CategoryStats.builder()
                .games(6).wins(5).draws(1).losses(0).winRate("1.000").build();
        UserDetailedStatsResponse.CategoryStats awayStats = UserDetailedStatsResponse.CategoryStats.builder()
                .games(4).wins(2).draws(0).losses(2).winRate("0.500").build();

        // when
        UserDetailedStatsResponse response = UserDetailedStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(10)
                .wins(7)
                .draws(1)
                .losses(2)
                .stadiumStats(new HashMap<>())
                .opponentStats(new HashMap<>())
                .dayOfWeekStats(new HashMap<>())
                .homeStats(homeStats)
                .awayStats(awayStats)
                .build();

        // then
        assertThat(response.getHomeStats().getWinRate()).isEqualTo("1.000");
        assertThat(response.getAwayStats().getWinRate()).isEqualTo("0.500");
        assertThat(response.getHomeStats().getGames()).isEqualTo(6);
        assertThat(response.getAwayStats().getGames()).isEqualTo(4);
    }

    @Test
    @DisplayName("NoArgsConstructor로 생성된 객체")
    void createWithNoArgsConstructor_Success() {
        // when
        UserDetailedStatsResponse response = new UserDetailedStatsResponse();

        // then
        assertThat(response.getUserId()).isNull();
        assertThat(response.getSeason()).isNull();
        assertThat(response.getTotalGames()).isEqualTo(0);
        assertThat(response.getStadiumStats()).isNull();
        assertThat(response.getOpponentStats()).isNull();
        assertThat(response.getDayOfWeekStats()).isNull();
        assertThat(response.getHomeStats()).isNull();
        assertThat(response.getAwayStats()).isNull();
    }

    @Test
    @DisplayName("CategoryStats NoArgsConstructor로 생성된 객체")
    void createCategoryStatsWithNoArgsConstructor_Success() {
        // when
        UserDetailedStatsResponse.CategoryStats categoryStats = new UserDetailedStatsResponse.CategoryStats();

        // then
        assertThat(categoryStats.getGames()).isEqualTo(0);
        assertThat(categoryStats.getWins()).isEqualTo(0);
        assertThat(categoryStats.getDraws()).isEqualTo(0);
        assertThat(categoryStats.getLosses()).isEqualTo(0);
        assertThat(categoryStats.getWinRate()).isNull();
    }
}
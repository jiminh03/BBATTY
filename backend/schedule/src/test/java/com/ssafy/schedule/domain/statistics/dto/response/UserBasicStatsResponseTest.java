package com.ssafy.schedule.domain.statistics.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserBasicStatsResponse DTO 테스트")
class UserBasicStatsResponseTest {

    @Test
    @DisplayName("UserBasicStatsResponse 빌더 패턴으로 생성")
    void buildUserBasicStatsResponse_Success() {
        // given & when
        UserBasicStatsResponse response = UserBasicStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(10)
                .wins(7)
                .draws(1)
                .losses(2)
                .winRate("0.778")
                .build();

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getSeason()).isEqualTo("2025");
        assertThat(response.getTotalGames()).isEqualTo(10);
        assertThat(response.getWins()).isEqualTo(7);
        assertThat(response.getDraws()).isEqualTo(1);
        assertThat(response.getLosses()).isEqualTo(2);
        assertThat(response.getWinRate()).isEqualTo("0.778");
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0.000",
            "1, 0, 1.000",
            "0, 1, 0.000",
            "3, 1, 0.750",
            "1, 2, 0.333",
            "2, 2, 0.500",
            "10, 5, 0.667",
            "1, 3, 0.250"
    })
    @DisplayName("승률 계산 - 다양한 승패 조합")
    void calculateWinRate_VariousCombinations(int wins, int losses, String expectedWinRate) {
        // when
        String actualWinRate = UserBasicStatsResponse.calculateWinRate(wins, losses);

        // then
        assertThat(actualWinRate).isEqualTo(expectedWinRate);
    }

    @Test
    @DisplayName("승률 계산 - 승부 결정 경기가 0인 경우")
    void calculateWinRate_NoDecisiveGames_ReturnsZero() {
        // when
        String winRate = UserBasicStatsResponse.calculateWinRate(0, 0);

        // then
        assertThat(winRate).isEqualTo("0.000");
    }

    @Test
    @DisplayName("승률 업데이트 메서드")
    void updateWinRate_Success() {
        // given
        UserBasicStatsResponse response = UserBasicStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(6)
                .wins(4)
                .draws(1)
                .losses(1)
                .winRate("0.000") // 임시값
                .build();

        // when
        response.updateWinRate();

        // then
        assertThat(response.getWinRate()).isEqualTo("0.800");
    }

    @Test
    @DisplayName("빈 통계 객체 생성")
    void createEmptyStats_Success() {
        // given
        Long userId = 123L;
        String season = "2025";

        // when
        UserBasicStatsResponse emptyStats = UserBasicStatsResponse.empty(userId, season);

        // then
        assertThat(emptyStats.getUserId()).isEqualTo(userId);
        assertThat(emptyStats.getSeason()).isEqualTo(season);
        assertThat(emptyStats.getTotalGames()).isEqualTo(0);
        assertThat(emptyStats.getWins()).isEqualTo(0);
        assertThat(emptyStats.getDraws()).isEqualTo(0);
        assertThat(emptyStats.getLosses()).isEqualTo(0);
        assertThat(emptyStats.getWinRate()).isEqualTo("0.000");
    }

    @Test
    @DisplayName("통산 시즌 통계 객체 생성")
    void createTotalSeasonStats_Success() {
        // given & when
        UserBasicStatsResponse totalStats = UserBasicStatsResponse.builder()
                .userId(1L)
                .season("total")
                .totalGames(100)
                .wins(60)
                .draws(10)
                .losses(30)
                .winRate("0.667")
                .build();

        // then
        assertThat(totalStats.getSeason()).isEqualTo("total");
        assertThat(totalStats.getTotalGames()).isEqualTo(100);
        assertThat(totalStats.getWins()).isEqualTo(60);
        assertThat(totalStats.getDraws()).isEqualTo(10);
        assertThat(totalStats.getLosses()).isEqualTo(30);
        assertThat(totalStats.getWinRate()).isEqualTo("0.667");
    }

    @Test
    @DisplayName("승률 업데이트 - 무승부만 있는 경우")
    void updateWinRate_OnlyDraws_ReturnsZero() {
        // given
        UserBasicStatsResponse response = UserBasicStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(5)
                .wins(0)
                .draws(5)
                .losses(0)
                .winRate("1.000") // 잘못된 초기값
                .build();

        // when
        response.updateWinRate();

        // then
        assertThat(response.getWinRate()).isEqualTo("0.000"); // 승부가 결정된 경기가 없으므로 0.000
    }

    @Test
    @DisplayName("전승 시 승률 계산")
    void calculateWinRate_PerfectRecord_ReturnsOne() {
        // given
        UserBasicStatsResponse response = UserBasicStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(10)
                .wins(8)
                .draws(2)
                .losses(0)
                .build();

        // when
        response.updateWinRate();

        // then
        assertThat(response.getWinRate()).isEqualTo("1.000");
    }

    @Test
    @DisplayName("전패 시 승률 계산")
    void calculateWinRate_AllLosses_ReturnsZero() {
        // given
        UserBasicStatsResponse response = UserBasicStatsResponse.builder()
                .userId(1L)
                .season("2025")
                .totalGames(8)
                .wins(0)
                .draws(3)
                .losses(5)
                .build();

        // when
        response.updateWinRate();

        // then
        assertThat(response.getWinRate()).isEqualTo("0.000");
    }

    @Test
    @DisplayName("NoArgsConstructor로 생성 후 값 설정")
    void createWithNoArgsConstructor_Success() {
        // given
        UserBasicStatsResponse response = new UserBasicStatsResponse();

        // when
        // Lombok의 @NoArgsConstructor로 생성된 객체는 모든 필드가 기본값
        
        // then
        assertThat(response.getUserId()).isNull();
        assertThat(response.getSeason()).isNull();
        assertThat(response.getTotalGames()).isEqualTo(0);
        assertThat(response.getWins()).isEqualTo(0);
        assertThat(response.getDraws()).isEqualTo(0);
        assertThat(response.getLosses()).isEqualTo(0);
        assertThat(response.getWinRate()).isNull();
    }

    @Test
    @DisplayName("AllArgsConstructor로 생성")
    void createWithAllArgsConstructor_Success() {
        // when
        UserBasicStatsResponse response = new UserBasicStatsResponse(
                2L, "2024", 15, 9, 4, 2, "0.692"
        );

        // then
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getSeason()).isEqualTo("2024");
        assertThat(response.getTotalGames()).isEqualTo(15);
        assertThat(response.getWins()).isEqualTo(9);
        assertThat(response.getDraws()).isEqualTo(2);
        assertThat(response.getLosses()).isEqualTo(4);
        assertThat(response.getWinRate()).isEqualTo("0.692");
    }
}
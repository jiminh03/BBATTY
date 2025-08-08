package com.ssafy.bbatty.domain.ranking.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GlobalRankingResponse 테스트")
class GlobalRankingResponseTest {

    private UserRankingDto createUserRankingDto(Long userId, String nickname, Double winRate, Integer rank) {
        return UserRankingDto.builder()
                .userId(userId)
                .nickname(nickname)
                .winRate(winRate)
                .rank(rank)
                .build();
    }

    @Test
    @DisplayName("Builder 패턴으로 객체 생성")
    void createWithBuilder_Success() {
        // given
        String expectedSeason = "2024";
        List<UserRankingDto> expectedRankings = Arrays.asList(
                createUserRankingDto(1L, "사용자1", 0.90, 1),
                createUserRankingDto(2L, "사용자2", 0.80, 2)
        );

        // when
        GlobalRankingResponse response = GlobalRankingResponse.builder()
                .season(expectedSeason)
                .rankings(expectedRankings)
                .build();

        // then
        assertThat(response.getSeason()).isEqualTo(expectedSeason);
        assertThat(response.getRankings()).isEqualTo(expectedRankings);
        assertThat(response.getRankings()).hasSize(2);
    }

    @Test
    @DisplayName("기본 생성자로 객체 생성")
    void createWithNoArgsConstructor_Success() {
        // given & when
        GlobalRankingResponse response = new GlobalRankingResponse();

        // then
        assertThat(response.getSeason()).isNull();
        assertThat(response.getRankings()).isNull();
    }

    @Test
    @DisplayName("전체 매개변수 생성자로 객체 생성")
    void createWithAllArgsConstructor_Success() {
        // given
        String season = "2023";
        List<UserRankingDto> rankings = Arrays.asList(
                createUserRankingDto(10L, "올아그스", 0.95, 1)
        );

        // when
        GlobalRankingResponse response = new GlobalRankingResponse(season, rankings);

        // then
        assertThat(response.getSeason()).isEqualTo(season);
        assertThat(response.getRankings()).isEqualTo(rankings);
        assertThat(response.getRankings()).hasSize(1);
    }

    @Test
    @DisplayName("빈 랭킹 리스트")
    void emptyRankingsList_Success() {
        // given & when
        GlobalRankingResponse response = GlobalRankingResponse.builder()
                .season("2024")
                .rankings(Arrays.asList())
                .build();

        // then
        assertThat(response.getSeason()).isEqualTo("2024");
        assertThat(response.getRankings()).isEmpty();
    }

    @Test
    @DisplayName("null 랭킹 리스트")
    void nullRankingsList_Success() {
        // given & when
        GlobalRankingResponse response = GlobalRankingResponse.builder()
                .season("2024")
                .rankings(null)
                .build();

        // then
        assertThat(response.getSeason()).isEqualTo("2024");
        assertThat(response.getRankings()).isNull();
    }

    @Test
    @DisplayName("다양한 시즌 형태")
    void variousSeasonFormats_Success() {
        // given
        List<String> seasons = Arrays.asList("2024", "2023-2024", "시즌1", null);

        for (String season : seasons) {
            // when
            GlobalRankingResponse response = GlobalRankingResponse.builder()
                    .season(season)
                    .rankings(Arrays.asList())
                    .build();

            // then
            assertThat(response.getSeason()).isEqualTo(season);
        }
    }

    @Test
    @DisplayName("대량의 랭킹 데이터 처리")
    void largeRankingData_Success() {
        // given
        List<UserRankingDto> largeRankings = Arrays.asList(
                createUserRankingDto(1L, "1위", 0.95, 1),
                createUserRankingDto(2L, "2위", 0.90, 2),
                createUserRankingDto(3L, "3위", 0.85, 3),
                createUserRankingDto(4L, "4위", 0.80, 4),
                createUserRankingDto(5L, "5위", 0.75, 5),
                createUserRankingDto(6L, "6위", 0.70, 6),
                createUserRankingDto(7L, "7위", 0.65, 7),
                createUserRankingDto(8L, "8위", 0.60, 8),
                createUserRankingDto(9L, "9위", 0.55, 9),
                createUserRankingDto(10L, "10위", 0.50, 10)
        );

        // when
        GlobalRankingResponse response = GlobalRankingResponse.builder()
                .season("2024")
                .rankings(largeRankings)
                .build();

        // then
        assertThat(response.getRankings()).hasSize(10);
        assertThat(response.getRankings().get(0).getRank()).isEqualTo(1);
        assertThat(response.getRankings().get(9).getRank()).isEqualTo(10);
    }

    @Test
    @DisplayName("Getter 메서드 테스트")
    void getterMethods_Success() {
        // given
        String expectedSeason = "테스트시즌";
        List<UserRankingDto> expectedRankings = Arrays.asList(
                createUserRankingDto(100L, "게터테스트", 0.77, 1)
        );

        GlobalRankingResponse response = GlobalRankingResponse.builder()
                .season(expectedSeason)
                .rankings(expectedRankings)
                .build();

        // when & then
        assertThat(response.getSeason()).isEqualTo(expectedSeason);
        assertThat(response.getRankings()).isEqualTo(expectedRankings);
        assertThat(response.getRankings().get(0).getNickname()).isEqualTo("게터테스트");
    }
}
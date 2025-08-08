package com.ssafy.bbatty.domain.ranking.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TeamRankingResponse 테스트")
class TeamRankingResponseTest {

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
        Long expectedTeamId = 1L;
        String expectedTeamName = "테스트 팀";
        String expectedSeason = "2024";
        List<UserRankingDto> expectedRankings = Arrays.asList(
                createUserRankingDto(1L, "팀원1", 0.90, 1),
                createUserRankingDto(2L, "팀원2", 0.80, 2)
        );

        // when
        TeamRankingResponse response = TeamRankingResponse.builder()
                .teamId(expectedTeamId)
                .teamName(expectedTeamName)
                .season(expectedSeason)
                .rankings(expectedRankings)
                .build();

        // then
        assertThat(response.getTeamId()).isEqualTo(expectedTeamId);
        assertThat(response.getTeamName()).isEqualTo(expectedTeamName);
        assertThat(response.getSeason()).isEqualTo(expectedSeason);
        assertThat(response.getRankings()).isEqualTo(expectedRankings);
        assertThat(response.getRankings()).hasSize(2);
    }

    @Test
    @DisplayName("기본 생성자로 객체 생성")
    void createWithNoArgsConstructor_Success() {
        // given & when
        TeamRankingResponse response = new TeamRankingResponse();

        // then
        assertThat(response.getTeamId()).isNull();
        assertThat(response.getTeamName()).isNull();
        assertThat(response.getSeason()).isNull();
        assertThat(response.getRankings()).isNull();
    }

    @Test
    @DisplayName("전체 매개변수 생성자로 객체 생성")
    void createWithAllArgsConstructor_Success() {
        // given
        Long teamId = 2L;
        String teamName = "올아그스 팀";
        String season = "2023";
        List<UserRankingDto> rankings = Arrays.asList(
                createUserRankingDto(10L, "올아그스멤버", 0.95, 1)
        );

        // when
        TeamRankingResponse response = new TeamRankingResponse(teamId, teamName, season, rankings);

        // then
        assertThat(response.getTeamId()).isEqualTo(teamId);
        assertThat(response.getTeamName()).isEqualTo(teamName);
        assertThat(response.getSeason()).isEqualTo(season);
        assertThat(response.getRankings()).isEqualTo(rankings);
        assertThat(response.getRankings()).hasSize(1);
    }

    @Test
    @DisplayName("빈 랭킹 리스트")
    void emptyRankingsList_Success() {
        // given & when
        TeamRankingResponse response = TeamRankingResponse.builder()
                .teamId(3L)
                .teamName("빈 팀")
                .season("2024")
                .rankings(Arrays.asList())
                .build();

        // then
        assertThat(response.getTeamId()).isEqualTo(3L);
        assertThat(response.getTeamName()).isEqualTo("빈 팀");
        assertThat(response.getSeason()).isEqualTo("2024");
        assertThat(response.getRankings()).isEmpty();
    }

    @Test
    @DisplayName("null 랭킹 리스트")
    void nullRankingsList_Success() {
        // given & when
        TeamRankingResponse response = TeamRankingResponse.builder()
                .teamId(4L)
                .teamName("널 팀")
                .season("2024")
                .rankings(null)
                .build();

        // then
        assertThat(response.getTeamId()).isEqualTo(4L);
        assertThat(response.getTeamName()).isEqualTo("널 팀");
        assertThat(response.getSeason()).isEqualTo("2024");
        assertThat(response.getRankings()).isNull();
    }

    @Test
    @DisplayName("다양한 팀명 형태")
    void variousTeamNames_Success() {
        // given
        List<String> teamNames = Arrays.asList(
                "KT 위즈",
                "LG 트윈스",
                "SSG 랜더스",
                "한국어팀명",
                "English Team",
                "123숫자팀",
                null
        );

        for (int i = 0; i < teamNames.size(); i++) {
            String teamName = teamNames.get(i);
            
            // when
            TeamRankingResponse response = TeamRankingResponse.builder()
                    .teamId((long) (i + 1))
                    .teamName(teamName)
                    .season("2024")
                    .rankings(Arrays.asList())
                    .build();

            // then
            assertThat(response.getTeamName()).isEqualTo(teamName);
        }
    }

    @Test
    @DisplayName("다양한 시즌 형태")
    void variousSeasonFormats_Success() {
        // given
        List<String> seasons = Arrays.asList("2024", "2023-2024", "시즌1", null);

        for (int i = 0; i < seasons.size(); i++) {
            String season = seasons.get(i);
            
            // when
            TeamRankingResponse response = TeamRankingResponse.builder()
                    .teamId((long) (i + 1))
                    .teamName("테스트팀")
                    .season(season)
                    .rankings(Arrays.asList())
                    .build();

            // then
            assertThat(response.getSeason()).isEqualTo(season);
        }
    }

    @Test
    @DisplayName("대량의 팀 랭킹 데이터 처리")
    void largeTeamRankingData_Success() {
        // given
        List<UserRankingDto> largeRankings = Arrays.asList(
                createUserRankingDto(1L, "팀원1", 0.95, 1),
                createUserRankingDto(2L, "팀원2", 0.90, 2),
                createUserRankingDto(3L, "팀원3", 0.85, 3),
                createUserRankingDto(4L, "팀원4", 0.80, 4),
                createUserRankingDto(5L, "팀원5", 0.75, 5),
                createUserRankingDto(6L, "팀원6", 0.70, 6),
                createUserRankingDto(7L, "팀원7", 0.65, 7),
                createUserRankingDto(8L, "팀원8", 0.60, 8),
                createUserRankingDto(9L, "팀원9", 0.55, 9),
                createUserRankingDto(10L, "팀원10", 0.50, 10)
        );

        // when
        TeamRankingResponse response = TeamRankingResponse.builder()
                .teamId(100L)
                .teamName("대형팀")
                .season("2024")
                .rankings(largeRankings)
                .build();

        // then
        assertThat(response.getRankings()).hasSize(10);
        assertThat(response.getRankings().get(0).getRank()).isEqualTo(1);
        assertThat(response.getRankings().get(9).getRank()).isEqualTo(10);
        assertThat(response.getRankings().get(0).getNickname()).isEqualTo("팀원1");
        assertThat(response.getRankings().get(9).getNickname()).isEqualTo("팀원10");
    }

    @Test
    @DisplayName("팀 ID가 0인 경우")
    void teamIdZero_Success() {
        // given & when
        TeamRankingResponse response = TeamRankingResponse.builder()
                .teamId(0L)
                .teamName("제로팀")
                .season("2024")
                .rankings(Arrays.asList())
                .build();

        // then
        assertThat(response.getTeamId()).isEqualTo(0L);
        assertThat(response.getTeamName()).isEqualTo("제로팀");
    }

    @Test
    @DisplayName("Getter 메서드 테스트")
    void getterMethods_Success() {
        // given
        Long expectedTeamId = 999L;
        String expectedTeamName = "게터테스트팀";
        String expectedSeason = "테스트시즌";
        List<UserRankingDto> expectedRankings = Arrays.asList(
                createUserRankingDto(100L, "게터테스트멤버", 0.77, 1)
        );

        TeamRankingResponse response = TeamRankingResponse.builder()
                .teamId(expectedTeamId)
                .teamName(expectedTeamName)
                .season(expectedSeason)
                .rankings(expectedRankings)
                .build();

        // when & then
        assertThat(response.getTeamId()).isEqualTo(expectedTeamId);
        assertThat(response.getTeamName()).isEqualTo(expectedTeamName);
        assertThat(response.getSeason()).isEqualTo(expectedSeason);
        assertThat(response.getRankings()).isEqualTo(expectedRankings);
        assertThat(response.getRankings().get(0).getNickname()).isEqualTo("게터테스트멤버");
    }
}
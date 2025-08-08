package com.ssafy.schedule.domain.statistics.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserStreakStatsResponse DTO 테스트")
class UserStreakStatsResponseTest {

    @Test
    @DisplayName("UserStreakStatsResponse 빌더 패턴으로 생성")
    void buildUserStreakStatsResponse_Success() {
        // given
        Map<String, Integer> maxWinStreakBySeason = new HashMap<>();
        maxWinStreakBySeason.put("2024", 8);
        maxWinStreakBySeason.put("2023", 5);
        maxWinStreakBySeason.put("2025", 3);

        // when
        UserStreakStatsResponse response = UserStreakStatsResponse.builder()
                .userId(1L)
                .currentSeason("2025")
                .currentWinStreak(3)
                .maxWinStreakAll(8)
                .maxWinStreakCurrentSeason(3)
                .maxWinStreakBySeason(maxWinStreakBySeason)
                .build();

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCurrentSeason()).isEqualTo("2025");
        assertThat(response.getCurrentWinStreak()).isEqualTo(3);
        assertThat(response.getMaxWinStreakAll()).isEqualTo(8);
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(3);
        assertThat(response.getMaxWinStreakBySeason()).hasSize(3);
        assertThat(response.getMaxWinStreakBySeason().get("2024")).isEqualTo(8);
    }

    @Test
    @DisplayName("빈 연승 통계 객체 생성")
    void createEmptyStreakStats_Success() {
        // given
        Long userId = 123L;
        String currentSeason = "2025";

        // when
        UserStreakStatsResponse emptyStats = UserStreakStatsResponse.empty(userId, currentSeason);

        // then
        assertThat(emptyStats.getUserId()).isEqualTo(userId);
        assertThat(emptyStats.getCurrentSeason()).isEqualTo(currentSeason);
        assertThat(emptyStats.getCurrentWinStreak()).isEqualTo(0);
        assertThat(emptyStats.getMaxWinStreakAll()).isEqualTo(0);
        assertThat(emptyStats.getMaxWinStreakCurrentSeason()).isEqualTo(0);
        assertThat(emptyStats.getMaxWinStreakBySeason()).isEmpty();
    }

    @Test
    @DisplayName("현재 연승 중인 사용자 통계")
    void currentWinStreakStats_Success() {
        // given & when
        UserStreakStatsResponse response = UserStreakStatsResponse.builder()
                .userId(1L)
                .currentSeason("2025")
                .currentWinStreak(5)
                .maxWinStreakAll(12)
                .maxWinStreakCurrentSeason(5)
                .maxWinStreakBySeason(Map.of(
                        "2025", 5,
                        "2024", 12,
                        "2023", 8
                ))
                .build();

        // then
        assertThat(response.getCurrentWinStreak()).isEqualTo(5);
        assertThat(response.getMaxWinStreakAll()).isEqualTo(12);
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(5);
        assertThat(response.getMaxWinStreakBySeason().get("2024")).isEqualTo(12);
    }

    @Test
    @DisplayName("연승이 끊어진 사용자 통계")
    void brokenWinStreakStats_Success() {
        // given & when
        UserStreakStatsResponse response = UserStreakStatsResponse.builder()
                .userId(1L)
                .currentSeason("2025")
                .currentWinStreak(0) // 연승 끊어짐
                .maxWinStreakAll(15)
                .maxWinStreakCurrentSeason(7)
                .maxWinStreakBySeason(Map.of(
                        "2025", 7,
                        "2024", 15,
                        "2023", 6
                ))
                .build();

        // then
        assertThat(response.getCurrentWinStreak()).isEqualTo(0);
        assertThat(response.getMaxWinStreakAll()).isEqualTo(15);
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(7);
    }

    @Test
    @DisplayName("첫 시즌 사용자의 연승 통계")
    void firstSeasonUserStats_Success() {
        // given & when
        UserStreakStatsResponse response = UserStreakStatsResponse.builder()
                .userId(1L)
                .currentSeason("2025")
                .currentWinStreak(4)
                .maxWinStreakAll(4)
                .maxWinStreakCurrentSeason(4)
                .maxWinStreakBySeason(Map.of("2025", 4))
                .build();

        // then
        assertThat(response.getCurrentWinStreak()).isEqualTo(4);
        assertThat(response.getMaxWinStreakAll()).isEqualTo(4);
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(4);
        assertThat(response.getMaxWinStreakBySeason()).hasSize(1);
        assertThat(response.getMaxWinStreakBySeason().get("2025")).isEqualTo(4);
    }

    @Test
    @DisplayName("다중 시즌 연승 기록")
    void multiSeasonStreakRecord_Success() {
        // given
        Map<String, Integer> maxWinStreakBySeason = new HashMap<>();
        maxWinStreakBySeason.put("2020", 3);
        maxWinStreakBySeason.put("2021", 7);
        maxWinStreakBySeason.put("2022", 12);
        maxWinStreakBySeason.put("2023", 9);
        maxWinStreakBySeason.put("2024", 15);
        maxWinStreakBySeason.put("2025", 6);

        // when
        UserStreakStatsResponse response = UserStreakStatsResponse.builder()
                .userId(1L)
                .currentSeason("2025")
                .currentWinStreak(2)
                .maxWinStreakAll(15) // 2024시즌 최고 기록
                .maxWinStreakCurrentSeason(6) // 현재 시즌 최고 기록
                .maxWinStreakBySeason(maxWinStreakBySeason)
                .build();

        // then
        assertThat(response.getMaxWinStreakBySeason()).hasSize(6);
        assertThat(response.getMaxWinStreakAll()).isEqualTo(15);
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(6);
        assertThat(response.getMaxWinStreakBySeason().get("2024")).isEqualTo(15);
        assertThat(response.getMaxWinStreakBySeason().get("2025")).isEqualTo(6);
    }

    @Test
    @DisplayName("현재 시즌이 최고 연승 기록인 경우")
    void currentSeasonBestRecord_Success() {
        // given & when
        UserStreakStatsResponse response = UserStreakStatsResponse.builder()
                .userId(1L)
                .currentSeason("2025")
                .currentWinStreak(10)
                .maxWinStreakAll(10) // 현재가 최고 기록
                .maxWinStreakCurrentSeason(10)
                .maxWinStreakBySeason(Map.of(
                        "2025", 10,
                        "2024", 8,
                        "2023", 5
                ))
                .build();

        // then
        assertThat(response.getCurrentWinStreak()).isEqualTo(10);
        assertThat(response.getMaxWinStreakAll()).isEqualTo(10);
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(10);
        assertThat(response.getMaxWinStreakBySeason().get("2025")).isEqualTo(10);
    }

    @Test
    @DisplayName("연승 기록이 없는 사용자")
    void noWinStreakUser_Success() {
        // given & when
        UserStreakStatsResponse response = UserStreakStatsResponse.builder()
                .userId(1L)
                .currentSeason("2025")
                .currentWinStreak(0)
                .maxWinStreakAll(0)
                .maxWinStreakCurrentSeason(0)
                .maxWinStreakBySeason(new HashMap<>())
                .build();

        // then
        assertThat(response.getCurrentWinStreak()).isEqualTo(0);
        assertThat(response.getMaxWinStreakAll()).isEqualTo(0);
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(0);
        assertThat(response.getMaxWinStreakBySeason()).isEmpty();
    }

    @Test
    @DisplayName("NoArgsConstructor로 생성된 객체")
    void createWithNoArgsConstructor_Success() {
        // when
        UserStreakStatsResponse response = new UserStreakStatsResponse();

        // then
        assertThat(response.getUserId()).isNull();
        assertThat(response.getCurrentSeason()).isNull();
        assertThat(response.getCurrentWinStreak()).isEqualTo(0);
        assertThat(response.getMaxWinStreakAll()).isEqualTo(0);
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(0);
        assertThat(response.getMaxWinStreakBySeason()).isNull();
    }

    @Test
    @DisplayName("AllArgsConstructor로 생성")
    void createWithAllArgsConstructor_Success() {
        // given
        Map<String, Integer> maxWinStreakBySeason = Map.of(
                "2025", 7,
                "2024", 12
        );

        // when
        UserStreakStatsResponse response = new UserStreakStatsResponse(
                1L, "2025", 7, 12, 7, maxWinStreakBySeason
        );

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCurrentSeason()).isEqualTo("2025");
        assertThat(response.getCurrentWinStreak()).isEqualTo(7);
        assertThat(response.getMaxWinStreakAll()).isEqualTo(12);
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(7);
        assertThat(response.getMaxWinStreakBySeason()).hasSize(2);
    }

    @Test
    @DisplayName("시즌별 연승 기록 추가 테스트")
    void seasonalStreakMapping_Success() {
        // given
        Map<String, Integer> seasonalStreaks = new HashMap<>();
        seasonalStreaks.put("2020", 2);
        seasonalStreaks.put("2021", 4);
        seasonalStreaks.put("2022", 8);
        seasonalStreaks.put("2023", 6);
        seasonalStreaks.put("2024", 11);
        seasonalStreaks.put("2025", 9);

        // when
        UserStreakStatsResponse response = UserStreakStatsResponse.builder()
                .userId(1L)
                .currentSeason("2025")
                .currentWinStreak(3)
                .maxWinStreakAll(11)
                .maxWinStreakCurrentSeason(9)
                .maxWinStreakBySeason(seasonalStreaks)
                .build();

        // then
        assertThat(response.getMaxWinStreakBySeason()).containsEntry("2024", 11);
        assertThat(response.getMaxWinStreakBySeason()).containsEntry("2025", 9);
        assertThat(response.getMaxWinStreakBySeason()).containsEntry("2020", 2);
        assertThat(response.getMaxWinStreakBySeason().size()).isEqualTo(6);
        
        // 전체 최고 연승은 2024년 기록
        assertThat(response.getMaxWinStreakAll()).isEqualTo(11);
        
        // 현재 시즌 최고 연승
        assertThat(response.getMaxWinStreakCurrentSeason()).isEqualTo(9);
    }
}
package com.ssafy.bbatty.domain.ranking.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserRankingDto 테스트")
class UserRankingDtoTest {

    @Test
    @DisplayName("Builder 패턴으로 객체 생성")
    void createWithBuilder_Success() {
        // given & when
        UserRankingDto dto = UserRankingDto.builder()
                .userId(1L)
                .nickname("테스트사용자")
                .winRate(0.85)
                .rank(1)
                .build();

        // then
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getNickname()).isEqualTo("테스트사용자");
        assertThat(dto.getWinRate()).isEqualTo(0.85);
        assertThat(dto.getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("기본 생성자로 객체 생성")
    void createWithNoArgsConstructor_Success() {
        // given & when
        UserRankingDto dto = new UserRankingDto();

        // then
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getNickname()).isNull();
        assertThat(dto.getWinRate()).isNull();
        assertThat(dto.getRank()).isNull();
    }

    @Test
    @DisplayName("전체 매개변수 생성자로 객체 생성")
    void createWithAllArgsConstructor_Success() {
        // given & when
        UserRankingDto dto = new UserRankingDto(2L, "사용자2", 0.75, 2);

        // then
        assertThat(dto.getUserId()).isEqualTo(2L);
        assertThat(dto.getNickname()).isEqualTo("사용자2");
        assertThat(dto.getWinRate()).isEqualTo(0.75);
        assertThat(dto.getRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("승률이 0인 경우")
    void winRateZero_Success() {
        // given & when
        UserRankingDto dto = UserRankingDto.builder()
                .userId(3L)
                .nickname("초보사용자")
                .winRate(0.0)
                .rank(10)
                .build();

        // then
        assertThat(dto.getWinRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("승률이 1인 경우")
    void winRateOne_Success() {
        // given & when
        UserRankingDto dto = UserRankingDto.builder()
                .userId(4L)
                .nickname("완벽사용자")
                .winRate(1.0)
                .rank(1)
                .build();

        // then
        assertThat(dto.getWinRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Getter 메서드 테스트")
    void getterMethods_Success() {
        // given
        Long expectedUserId = 5L;
        String expectedNickname = "게터테스트";
        Double expectedWinRate = 0.65;
        Integer expectedRank = 5;

        UserRankingDto dto = UserRankingDto.builder()
                .userId(expectedUserId)
                .nickname(expectedNickname)
                .winRate(expectedWinRate)
                .rank(expectedRank)
                .build();

        // when & then
        assertThat(dto.getUserId()).isEqualTo(expectedUserId);
        assertThat(dto.getNickname()).isEqualTo(expectedNickname);
        assertThat(dto.getWinRate()).isEqualTo(expectedWinRate);
        assertThat(dto.getRank()).isEqualTo(expectedRank);
    }
}
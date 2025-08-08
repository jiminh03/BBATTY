package com.ssafy.bbatty.domain.ranking.controller;

import com.ssafy.bbatty.domain.ranking.dto.response.GlobalRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.TeamRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.UserRankingDto;
import com.ssafy.bbatty.domain.ranking.service.RankingService;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.constants.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingController 테스트")
class RankingControllerTest {

    @Mock
    private RankingService rankingService;

    @InjectMocks
    private RankingController rankingController;

    private UserRankingDto createUserRankingDto(Long userId, String nickname, Double winRate, Integer rank) {
        return UserRankingDto.builder()
                .userId(userId)
                .nickname(nickname)
                .winRate(winRate)
                .rank(rank)
                .build();
    }

    @Test
    @DisplayName("전체 승률 랭킹 조회 - 성공")
    void getGlobalWinRateRanking_Success() {
        // given
        List<UserRankingDto> rankings = Arrays.asList(
                createUserRankingDto(1L, "사용자1", 0.90, 1),
                createUserRankingDto(2L, "사용자2", 0.80, 2),
                createUserRankingDto(3L, "사용자3", 0.75, 3)
        );

        GlobalRankingResponse expectedResponse = GlobalRankingResponse.builder()
                .season("2024")
                .rankings(rankings)
                .build();

        given(rankingService.getGlobalWinRateRanking()).willReturn(expectedResponse);

        // when
        ResponseEntity<ApiResponse<GlobalRankingResponse>> result = rankingController.getGlobalWinRateRanking();

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().status()).isEqualTo(Status.SUCCESS);
        assertThat(result.getBody().data().getSeason()).isEqualTo("2024");
        assertThat(result.getBody().data().getRankings()).hasSize(3);
        assertThat(result.getBody().data().getRankings().get(0).getUserId()).isEqualTo(1L);
        assertThat(result.getBody().data().getRankings().get(0).getNickname()).isEqualTo("사용자1");
        assertThat(result.getBody().data().getRankings().get(0).getWinRate()).isEqualTo(0.90);
        assertThat(result.getBody().data().getRankings().get(0).getRank()).isEqualTo(1);

        verify(rankingService).getGlobalWinRateRanking();
    }

    @Test
    @DisplayName("전체 승률 랭킹 조회 - 빈 랭킹")
    void getGlobalWinRateRanking_EmptyRanking() {
        // given
        GlobalRankingResponse expectedResponse = GlobalRankingResponse.builder()
                .season("2024")
                .rankings(Arrays.asList())
                .build();

        given(rankingService.getGlobalWinRateRanking()).willReturn(expectedResponse);

        // when
        ResponseEntity<ApiResponse<GlobalRankingResponse>> result = rankingController.getGlobalWinRateRanking();

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().status()).isEqualTo(Status.SUCCESS);
        assertThat(result.getBody().data().getSeason()).isEqualTo("2024");
        assertThat(result.getBody().data().getRankings()).isEmpty();

        verify(rankingService).getGlobalWinRateRanking();
    }

    @Test
    @DisplayName("팀별 승률 랭킹 조회 - 성공")
    void getTeamWinRateRanking_Success() {
        // given
        Long teamId = 1L;
        List<UserRankingDto> rankings = Arrays.asList(
                createUserRankingDto(1L, "팀원1", 0.95, 1),
                createUserRankingDto(2L, "팀원2", 0.85, 2)
        );

        TeamRankingResponse expectedResponse = TeamRankingResponse.builder()
                .teamId(teamId)
                .teamName("테스트 팀")
                .season("2024")
                .rankings(rankings)
                .build();

        given(rankingService.getTeamWinRateRanking(teamId)).willReturn(expectedResponse);

        // when
        ResponseEntity<ApiResponse<TeamRankingResponse>> result = rankingController.getTeamWinRateRanking(teamId);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().status()).isEqualTo(Status.SUCCESS);
        assertThat(result.getBody().data().getTeamId()).isEqualTo(teamId);
        assertThat(result.getBody().data().getTeamName()).isEqualTo("테스트 팀");
        assertThat(result.getBody().data().getSeason()).isEqualTo("2024");
        assertThat(result.getBody().data().getRankings()).hasSize(2);
        assertThat(result.getBody().data().getRankings().get(0).getUserId()).isEqualTo(1L);
        assertThat(result.getBody().data().getRankings().get(0).getNickname()).isEqualTo("팀원1");

        verify(rankingService).getTeamWinRateRanking(teamId);
    }

    @Test
    @DisplayName("팀별 승률 랭킹 조회 - 빈 랭킹")
    void getTeamWinRateRanking_EmptyRanking() {
        // given
        Long teamId = 2L;
        TeamRankingResponse expectedResponse = TeamRankingResponse.builder()
                .teamId(teamId)
                .teamName("빈 팀")
                .season("2024")
                .rankings(Arrays.asList())
                .build();

        given(rankingService.getTeamWinRateRanking(teamId)).willReturn(expectedResponse);

        // when
        ResponseEntity<ApiResponse<TeamRankingResponse>> result = rankingController.getTeamWinRateRanking(teamId);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().status()).isEqualTo(Status.SUCCESS);
        assertThat(result.getBody().data().getTeamId()).isEqualTo(teamId);
        assertThat(result.getBody().data().getTeamName()).isEqualTo("빈 팀");
        assertThat(result.getBody().data().getSeason()).isEqualTo("2024");
        assertThat(result.getBody().data().getRankings()).isEmpty();

        verify(rankingService).getTeamWinRateRanking(teamId);
    }

    @Test
    @DisplayName("서비스 메서드 호출 검증")
    void verifyServiceMethodCalls() {
        // given
        Long teamId = 1L;
        GlobalRankingResponse globalResponse = GlobalRankingResponse.builder()
                .season("2024")
                .rankings(Arrays.asList())
                .build();
        TeamRankingResponse teamResponse = TeamRankingResponse.builder()
                .teamId(teamId)
                .teamName("테스트 팀")
                .season("2024")
                .rankings(Arrays.asList())
                .build();

        given(rankingService.getGlobalWinRateRanking()).willReturn(globalResponse);
        given(rankingService.getTeamWinRateRanking(teamId)).willReturn(teamResponse);

        // when
        rankingController.getGlobalWinRateRanking();
        rankingController.getTeamWinRateRanking(teamId);

        // then
        verify(rankingService).getGlobalWinRateRanking();
        verify(rankingService).getTeamWinRateRanking(teamId);
    }
}
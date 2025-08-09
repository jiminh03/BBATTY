package com.ssafy.bbatty.domain.ranking.service;

import com.ssafy.bbatty.domain.ranking.dto.response.GlobalRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.TeamRankingResponse;
import com.ssafy.bbatty.domain.ranking.dto.response.UserRankingDto;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.constants.RedisKey;
import com.ssafy.bbatty.global.constants.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingServiceImpl 테스트")
class RankingServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private RankingServiceImpl rankingService;

    private User createTestUser(Long id, String nickname) {
        Team team = Team.builder()
                .id(1L)
                .name("LG 트윈스")
                .build();

        return User.builder()
                .id(id)
                .nickname(nickname)
                .team(team)
                .gender(Gender.MALE)
                .birthYear(1990)
                .profileImg("profile.jpg")
                .introduction("안녕하세요")
                .role(Role.USER)
                .postsPublic(true)
                .statsPublic(true)
                .attendanceRecordsPublic(true)
                .build();
    }

    private Team createTestTeam(Long id, String name) {
        return Team.builder()
                .id(id)
                .name(name)
                .build();
    }

    @Test
    @DisplayName("전체 승률 랭킹 조회 - 데이터가 있는 경우")
    void getGlobalWinRateRanking_WithData_Success() {
        // given
        User user1 = createTestUser(1L, "사용자1");
        User user2 = createTestUser(2L, "사용자2");
        
        Set<ZSetOperations.TypedTuple<Object>> rankingData = new HashSet<>();
        rankingData.add(new DefaultTypedTuple<>("1", 0.85));
        rankingData.add(new DefaultTypedTuple<>("2", 0.75));

        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(RedisKey.RANKING_GLOBAL_TOP10, 0, 9))
                .willReturn(rankingData);
        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(userRepository.findById(2L)).willReturn(Optional.of(user2));

        // when
        GlobalRankingResponse result = rankingService.getGlobalWinRateRanking();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSeason()).isEqualTo(String.valueOf(LocalDate.now().getYear()));
        assertThat(result.getRankings()).hasSize(2);
        
        UserRankingDto firstRanking = result.getRankings().get(0);
        assertThat(firstRanking.getUserId()).isEqualTo(1L);
        assertThat(firstRanking.getNickname()).isEqualTo("사용자1");
        assertThat(firstRanking.getWinRate()).isEqualTo(0.85);
        assertThat(firstRanking.getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("전체 승률 랭킹 조회 - 데이터가 없는 경우")
    void getGlobalWinRateRanking_NoData_ReturnEmptyList() {
        // given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(RedisKey.RANKING_GLOBAL_TOP10, 0, 9))
                .willReturn(null);

        // when
        GlobalRankingResponse result = rankingService.getGlobalWinRateRanking();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSeason()).isEqualTo(String.valueOf(LocalDate.now().getYear()));
        assertThat(result.getRankings()).isEmpty();
    }

    @Test
    @DisplayName("전체 승률 랭킹 조회 - 사용자 정보가 없는 경우")
    void getGlobalWinRateRanking_UserNotFound_SkipUser() {
        // given
        Set<ZSetOperations.TypedTuple<Object>> rankingData = new HashSet<>();
        rankingData.add(new DefaultTypedTuple<>("999", 0.85)); // 존재하지 않는 사용자

        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(RedisKey.RANKING_GLOBAL_TOP10, 0, 9))
                .willReturn(rankingData);
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        GlobalRankingResponse result = rankingService.getGlobalWinRateRanking();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRankings()).isEmpty(); // 사용자 정보가 없으면 스킵됨
    }

    @Test
    @DisplayName("팀별 승률 랭킹 조회 - 데이터가 있는 경우")
    void getTeamWinRateRanking_WithData_Success() {
        // given
        Long teamId = 1L;
        Team team = createTestTeam(teamId, "테스트 팀");
        User user1 = createTestUser(1L, "팀원1");
        User user2 = createTestUser(2L, "팀원2");
        
        Set<ZSetOperations.TypedTuple<Object>> rankingData = new LinkedHashSet<>();
        rankingData.add(new DefaultTypedTuple<>("1", 0.90));
        rankingData.add(new DefaultTypedTuple<>("2", 0.80));

        String teamRankingKey = RedisKey.RANKING_TEAM_TOP10 + teamId + ":top10";
        
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(teamRankingKey, 0, 9))
                .willReturn(rankingData);
        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(userRepository.findById(2L)).willReturn(Optional.of(user2));
        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));

        // when
        TeamRankingResponse result = rankingService.getTeamWinRateRanking(teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTeamId()).isEqualTo(teamId);
        assertThat(result.getTeamName()).isEqualTo("테스트 팀");
        assertThat(result.getSeason()).isEqualTo(String.valueOf(LocalDate.now().getYear()));
        assertThat(result.getRankings()).hasSize(2);
        
        UserRankingDto firstRanking = result.getRankings().get(0);
        assertThat(firstRanking.getUserId()).isEqualTo(1L);
        assertThat(firstRanking.getNickname()).isEqualTo("팀원1");
        assertThat(firstRanking.getWinRate()).isEqualTo(0.90);
        assertThat(firstRanking.getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("팀별 승률 랭킹 조회 - 데이터가 없는 경우")
    void getTeamWinRateRanking_NoData_ReturnEmptyList() {
        // given
        Long teamId = 1L;
        Team team = createTestTeam(teamId, "테스트 팀");
        String teamRankingKey = RedisKey.RANKING_TEAM_TOP10 + teamId + ":top10";
        
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(teamRankingKey, 0, 9))
                .willReturn(null);
        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));

        // when
        TeamRankingResponse result = rankingService.getTeamWinRateRanking(teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTeamId()).isEqualTo(teamId);
        assertThat(result.getTeamName()).isEqualTo("테스트 팀");
        assertThat(result.getRankings()).isEmpty();
    }

    @Test
    @DisplayName("팀별 승률 랭킹 조회 - 팀이 존재하지 않는 경우")
    void getTeamWinRateRanking_TeamNotFound_UseUnknownTeamName() {
        // given
        Long teamId = 999L;
        String teamRankingKey = RedisKey.RANKING_TEAM_TOP10 + teamId + ":top10";
        
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(teamRankingKey, 0, 9))
                .willReturn(null);
        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        // when
        TeamRankingResponse result = rankingService.getTeamWinRateRanking(teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTeamId()).isEqualTo(teamId);
        assertThat(result.getTeamName()).isEqualTo("Unknown Team");
        assertThat(result.getRankings()).isEmpty();
    }

    @Test
    @DisplayName("팀별 승률 랭킹 조회 - 사용자 정보가 없는 경우")
    void getTeamWinRateRanking_UserNotFound_SkipUser() {
        // given
        Long teamId = 1L;
        Team team = createTestTeam(teamId, "테스트 팀");
        
        Set<ZSetOperations.TypedTuple<Object>> rankingData = new HashSet<>();
        rankingData.add(new DefaultTypedTuple<>("999", 0.85)); // 존재하지 않는 사용자

        String teamRankingKey = RedisKey.RANKING_TEAM_TOP10 + teamId + ":top10";
        
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(teamRankingKey, 0, 9))
                .willReturn(rankingData);
        given(userRepository.findById(999L)).willReturn(Optional.empty());
        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));

        // when
        TeamRankingResponse result = rankingService.getTeamWinRateRanking(teamId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRankings()).isEmpty(); // 사용자 정보가 없으면 스킵됨
    }

    @Test
    @DisplayName("랭킹 순위가 올바르게 부여되는지 확인")
    void rankingOrder_CorrectOrder() {
        // given
        User user1 = createTestUser(1L, "1위사용자");
        User user2 = createTestUser(2L, "2위사용자");
        User user3 = createTestUser(3L, "3위사용자");
        
        Set<ZSetOperations.TypedTuple<Object>> rankingData = new HashSet<>();
        // Redis는 내림차순으로 정렬되어 반환됨
        rankingData.add(new DefaultTypedTuple<>("1", 0.95)); // 1위
        rankingData.add(new DefaultTypedTuple<>("2", 0.85)); // 2위
        rankingData.add(new DefaultTypedTuple<>("3", 0.75)); // 3위

        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRangeWithScores(RedisKey.RANKING_GLOBAL_TOP10, 0, 9))
                .willReturn(rankingData);
        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(userRepository.findById(2L)).willReturn(Optional.of(user2));
        given(userRepository.findById(3L)).willReturn(Optional.of(user3));

        // when
        GlobalRankingResponse result = rankingService.getGlobalWinRateRanking();

        // then
        assertThat(result.getRankings()).hasSize(3);
        
        // 순위가 올바르게 부여되었는지 확인
        for (int i = 0; i < result.getRankings().size(); i++) {
            UserRankingDto ranking = result.getRankings().get(i);
            assertThat(ranking.getRank()).isEqualTo(i + 1);
        }
    }
}
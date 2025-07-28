package com.ssafy.bbatty.global.config;

import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.team.repository.TeamRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.entity.UserInfo;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.domain.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductInitializer implements CommandLineRunner {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("ProductInitializer 시작됩니다.");
        try {
            initializeTeams();
            initializeUsers();
            log.info("ProductInitializer 완료되었습니다.");
        } catch (Exception e) {
            log.error("ProductInitializer 실행 중 오류 발생", e);
            throw e;
        }
    }

    private void initializeTeams() {
        if (teamRepository.count() > 0) {
            log.info("Team 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("KBO 10개 팀 데이터 초기화를 시작합니다.");

        List<Team> teams = List.of(
            Team.builder()
                .name("KIA 타이거즈")
                .wins(87)
                .draws(2)
                .loses(55)
                .rank(1)
                .winRate(new BigDecimal("0.613"))
                .gb(new BigDecimal("0.0"))
                .build(),
            Team.builder()
                .name("삼성 라이온즈")
                .wins(83)
                .draws(1)
                .loses(60)
                .rank(2)
                .winRate(new BigDecimal("0.580"))
                .gb(new BigDecimal("4.5"))
                .build(),
            Team.builder()
                .name("LG 트윈스")
                .wins(79)
                .draws(2)
                .loses(63)
                .rank(3)
                .winRate(new BigDecimal("0.556"))
                .gb(new BigDecimal("8.0"))
                .build(),
            Team.builder()
                .name("두산 베어스")
                .wins(76)
                .draws(3)
                .loses(65)
                .rank(4)
                .winRate(new BigDecimal("0.539"))
                .gb(new BigDecimal("11.0"))
                .build(),
            Team.builder()
                .name("KT 위즈")
                .wins(75)
                .draws(1)
                .loses(68)
                .rank(5)
                .winRate(new BigDecimal("0.524"))
                .gb(new BigDecimal("12.5"))
                .build(),
            Team.builder()
                .name("SSG 랜더스")
                .wins(72)
                .draws(2)
                .loses(70)
                .rank(6)
                .winRate(new BigDecimal("0.507"))
                .gb(new BigDecimal("15.5"))
                .build(),
            Team.builder()
                .name("롯데 자이언츠")
                .wins(70)
                .draws(1)
                .loses(73)
                .rank(7)
                .winRate(new BigDecimal("0.490"))
                .gb(new BigDecimal("18.0"))
                .build(),
            Team.builder()
                .name("한화 이글스")
                .wins(66)
                .draws(2)
                .loses(76)
                .rank(8)
                .winRate(new BigDecimal("0.465"))
                .gb(new BigDecimal("22.0"))
                .build(),
            Team.builder()
                .name("NC 다이노스")
                .wins(63)
                .draws(1)
                .loses(80)
                .rank(9)
                .winRate(new BigDecimal("0.441"))
                .gb(new BigDecimal("25.5"))
                .build(),
            Team.builder()
                .name("키움 히어로즈")
                .wins(61)
                .draws(3)
                .loses(80)
                .rank(10)
                .winRate(new BigDecimal("0.434"))
                .gb(new BigDecimal("27.0"))
                .build()
        );

        teamRepository.saveAll(teams);
        log.info("KBO 10개 팀 데이터 초기화가 완료되었습니다.");
    }

    private void initializeUsers() {
        if (userRepository.count() > 0) {
            log.info("User 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("더미 사용자 데이터 초기화를 시작합니다.");

        // 팀 데이터를 먼저 조회
        List<Team> teams = teamRepository.findAll();
        if (teams.isEmpty()) {
            log.error("팀 데이터가 없어서 사용자 초기화를 건너뜁니다.");
            return;
        }

        // 각 팀마다 5명씩 더미 사용자 생성
        for (Team team : teams) {
            for (int i = 1; i <= 5; i++) {
                // User 생성
                User user = User.builder()
                        .nickname(team.getName().replace(" ", "") + "팬" + i)
                        .gender(i % 2 == 0 ? User.Gender.MALE : User.Gender.FEMALE)
                        .age(20 + (i * 3))
                        .team(team)
                        .introduction(team.getName() + "을 사랑하는 팬입니다!")
                        .role(User.Role.USER)
                        .profileImg("https://example.com/profile" + i + ".jpg")
                        .build();

                User savedUser = userRepository.save(user);

                // UserInfo 생성 (양방향 연관관계 설정)
                UserInfo userInfo = UserInfo.createUserInfo(
                        savedUser,
                        "kakao_" + team.getId() + "_" + i,
                        team.getName().replace(" ", "").toLowerCase() + "fan" + i + "@example.com"
                );

                userInfoRepository.save(userInfo);
            }
        }

        // 관리자 계정 생성
        Team kiaTeam = teams.get(0); // KIA 타이거즈
        User adminUser = User.builder()
                .nickname("관리자")
                .gender(User.Gender.MALE)
                .age(30)
                .team(kiaTeam)
                .introduction("시스템 관리자입니다.")
                .role(User.Role.ADMIN)
                .profileImg("https://example.com/admin.jpg")
                .build();

        User savedAdmin = userRepository.save(adminUser);

        UserInfo adminUserInfo = UserInfo.createUserInfo(
                savedAdmin,
                "kakao_admin",
                "admin@bbatty.com"
        );

        userInfoRepository.save(adminUserInfo);

        log.info("더미 사용자 데이터 초기화가 완료되었습니다. (총 {}명)", userRepository.count());
    }
}

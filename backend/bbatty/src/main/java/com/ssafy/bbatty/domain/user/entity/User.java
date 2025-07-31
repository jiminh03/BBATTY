package com.ssafy.bbatty.domain.user.entity;

import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.constants.Role;
import com.ssafy.bbatty.global.entity.BaseEntity;
import com.ssafy.bbatty.global.exception.ApiException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "user", schema = "BBATTY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "birth_year", nullable = false)
    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Nullable
    @Column(name = "profile_img")
    private String profileImg;

    @Nullable
    @Column(name = "introduction")
    private String introduction;

    // UserInfo와 1:1 관계
    @Setter
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserInfo userInfo;

    // 유틸리티 메서드들
    
    /**
     * 카카오 생년 문자열을 정수로 파싱
     * @param kakaoBirthyear 카카오에서 받은 생년월일 문자열
     * @return 생년 정수값
     * @throws ApiException 유효하지 않은 값이거나 null인 경우
     */
    public static int parseBirthYear(String kakaoBirthyear) {
        if (kakaoBirthyear == null || kakaoBirthyear.trim().isEmpty()) {
            throw new ApiException(ErrorCode.KAKAO_BIRTH_INFO_REQUIRED);
        }

        try {
            return Integer.parseInt(kakaoBirthyear.trim());
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.KAKAO_BIRTH_INFO_INVALID);
        }
    }

    /**
     * 카카오 정보로 사용자 생성 (메인 팩토리 메서드)
     * AuthService에서 검증된 데이터를 받아서 User 생성
     */
    public static User createFromKakao(String nickname, Gender gender, Integer birthYear,
                                       Team team, String profileImg) {
        return User.builder()
                .nickname(nickname)
                .gender(gender)      // AuthService에서 이미 검증됨
                .birthYear(birthYear) // AuthService에서 이미 검증됨
                .team(team)
                .role(Role.USER)
                .build();
    }

    /**
     * 프로필 업데이트
     */
    public void updateProfile(String nickname, String introduction, String profileImg) {
        this.nickname = nickname;
        this.introduction = introduction;
        this.profileImg = profileImg;
    }

    /**
     * 나이 계산 (현재 년도 기준)
     */
    public int getAge() {
        return java.time.LocalDate.now().getYear() - this.birthYear + 1;
    }

    /**
     * 팀 ID 반환
     */
    public Long getTeamId() {
        return this.team != null ? this.team.getId() : null;
    }
}
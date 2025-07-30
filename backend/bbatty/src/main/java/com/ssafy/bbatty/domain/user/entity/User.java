package com.ssafy.bbatty.domain.user.entity;

import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.entity.BaseEntity;
import com.ssafy.bbatty.global.exception.ApiException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

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

    // Enum 정의
    public enum Gender {
        MALE, FEMALE
    }

    public enum Role {
        USER, ADMIN
    }

    // 유틸리티 메서드
    public static Gender parseGender(String kakaoGender) {
        if (kakaoGender == null) {
            throw new ApiException(ErrorCode.KAKAO_GENDER_INFO_REQUIRED);
        }

        return switch (kakaoGender.toLowerCase()) {
            case "female" -> Gender.FEMALE;
            case "male" -> Gender.MALE;
            default -> throw new ApiException(ErrorCode.KAKAO_GENDER_INFO_INVALID);
        };
    }

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

    public static User createUser(String nickname, Gender gender, Integer birthYear, Team team,
                                  String introduction, String profileImg) {
        return User.builder()
                .nickname(nickname)
                .gender(gender)
                .birthYear(birthYear)
                .team(team)
                .introduction(introduction)  // null 가능 (필드에 @Nullable 있음)
                .profileImg(profileImg)      // null 가능 (필드에 @Nullable 있음)
                .role(Role.USER)
                .build();
    }

    public void updateProfile(String nickname, String introduction, String profileImg) {
        this.nickname = nickname;
        this.introduction = introduction;
        this.profileImg = profileImg;
    }
}
package com.ssafy.bbatty.domain.user.entity;

import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "user", schema = "BBATTY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

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

    @Column(name = "age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "profile_img")
    private String profileImg;

    @Lob
    @Column(name = "introduction")
    private String introduction;

    // UserInfo와 1:1 관계
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserInfo userInfo;

    // Enum 정의
    public enum Gender {
        MALE, FEMALE
    }

    public enum Role {
        USER, ADMIN
    }

    // 유틸리티 메서드
    public static int calculateAge(String birthyear, String birthday) {
        if (birthyear == null || birthday == null) {
            return 25; // 기본값
        }
        
        try {
            // birthday는 "MMDD" 형식
            String month = birthday.substring(0, 2);
            String day = birthday.substring(2, 4);
            
            LocalDate birthDate = LocalDate.of(
                Integer.parseInt(birthyear),
                Integer.parseInt(month), 
                Integer.parseInt(day)
            );
            
            return Period.between(birthDate, LocalDate.now()).getYears();
        } catch (Exception e) {
            return 25; // 파싱 실패 시 기본값
        }
    }

    public static Gender parseGender(String kakaoGender) {
        if (kakaoGender == null) {
            return Gender.MALE; // 기본값
        }
        
        switch (kakaoGender.toLowerCase()) {
            case "female":
                return Gender.FEMALE;
            case "male":
                return Gender.MALE;
            default:
                return Gender.MALE; // 기본값
        }
    }

    // 비즈니스 메서드
    public static User createUser(String nickname, Gender gender, Integer age, Team team, String introduction) {
        return User.builder()
                .nickname(nickname)
                .gender(gender)
                .age(age)
                .team(team)
                .introduction(introduction)
                .role(Role.USER)
                .build();
    }

    public void updateProfile(String nickname, String introduction, String profileImg) {
        this.nickname = nickname;
        this.introduction = introduction;
        this.profileImg = profileImg;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
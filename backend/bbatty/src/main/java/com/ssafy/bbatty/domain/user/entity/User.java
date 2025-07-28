package com.ssafy.bbatty.domain.user.entity;

import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user", schema = "myapp_db")
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
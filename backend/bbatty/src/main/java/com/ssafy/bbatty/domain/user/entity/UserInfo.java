package com.ssafy.bbatty.domain.user.entity;

import com.ssafy.bbatty.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_info", schema = "myapp_db")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserInfo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "kakao_id", length = 50)
    private String kakaoId;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    // 비즈니스 메서드
    public static UserInfo createUserInfo(User user, String kakaoId, String email) {
        UserInfo userInfo = UserInfo.builder()
                .user(user)
                .kakaoId(kakaoId)
                .email(email)
                .build();

        // 양방향 연관관계 설정
        user.setUserInfo(userInfo);
        return userInfo;
    }
}
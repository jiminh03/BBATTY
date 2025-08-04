package com.ssafy.bbatty.domain.attendance.entity;

import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 직관 기록 엔티티
 * - 복합키로 중복 직관 방지
 * - 순수 로그 데이터로 불변성 보장
 */
@Entity
@Table(name = "user_attended")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(UserAttendedId.class)
public class UserAttended {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "game_id")
    private Long gameId;

    // 연관관계 매핑 (조회용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", insertable = false, updatable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder
    public UserAttended(Long userId, Long gameId) {
        this.userId = userId;
        this.gameId = gameId;
    }

    /**
     * 직관 기록 생성 팩토리 메서드
     */
    public static UserAttended of(Long userId, Long gameId) {
        return UserAttended.builder()
                .userId(userId)
                .gameId(gameId)
                .build();
    }
}
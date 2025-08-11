package com.ssafy.schedule.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user", schema = "BBATTY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;
}
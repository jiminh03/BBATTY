package com.ssafy.bbatty.domain.team.entity;

import com.ssafy.bbatty.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

/**
 * 팀 정보 엔티티
 */
@Entity
@Table(name = "team", schema = "BBATTY")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @ColumnDefault("0")
    @Column(name = "wins", nullable = false)
    private Integer wins;

    @ColumnDefault("0")
    @Column(name = "draws", nullable = false)
    private Integer draws;

    @ColumnDefault("0")
    @Column(name = "loses", nullable = false)
    private Integer loses;

    @Column(name = "team_rank")
    private Integer teamRank;

    @Column(name = "win_rate", precision = 5, scale = 3)
    private BigDecimal winRate;

    @Column(name = "gb", precision = 3, scale = 1)
    private BigDecimal gb;

    // 비즈니스 메서드
    public void updateStats(int wins, int draws, int loses, int teamRank, BigDecimal winRate, BigDecimal gb) {
        this.wins = wins;
        this.draws = draws;
        this.loses = loses;
        this.teamRank = teamRank;
        this.winRate = winRate;
        this.gb = gb;
    }
}
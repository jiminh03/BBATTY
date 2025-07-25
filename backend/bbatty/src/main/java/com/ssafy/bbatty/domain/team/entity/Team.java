package com.ssafy.bbatty.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "team", schema = "BBATTY")
public class Team {
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

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "win_rate", precision = 5, scale = 3)
    private BigDecimal winRate;

    @Column(name = "gb", precision = 3, scale = 1)
    private BigDecimal gb;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

}
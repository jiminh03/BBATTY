package com.ssafy.schedule.entity;

import com.ssafy.schedule.common.GameResult;
import com.ssafy.schedule.common.GameStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GameStatus status = GameStatus.SCHEDULED;

    @Column(name = "away_score")
    private Integer awayScore = 0;

    @Column(name = "home_score")
    private Integer homeScore = 0;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "result")
    private GameResult result;

    @Column(name = "double_header", nullable = false)
    private Boolean doubleHeader = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Game() {}

    public Game(Team awayTeam, Team homeTeam, LocalDateTime dateTime) {
        this.awayTeam = awayTeam;
        this.homeTeam = homeTeam;
        this.dateTime = dateTime;
    }

    public Game(Team awayTeam, Team homeTeam, LocalDateTime dateTime, Boolean doubleHeader) {
        this.awayTeam = awayTeam;
        this.homeTeam = homeTeam;
        this.dateTime = dateTime;
        this.doubleHeader = doubleHeader;
    }
}


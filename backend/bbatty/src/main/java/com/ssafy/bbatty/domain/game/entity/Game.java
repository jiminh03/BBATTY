package com.ssafy.bbatty.domain.game.entity;

import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.global.constants.GameResult;
import com.ssafy.bbatty.global.constants.GameStatus;
import com.ssafy.bbatty.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 경기 정보 엔티티
 */
@Entity
@Table(name = "game")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game extends BaseEntity {

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
    @Column(nullable = false)
    private GameStatus status;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    private GameResult result;

    @Column(nullable = false, length = 50)
    private String stadium;

    @Column(precision = 10, scale = 7)
    private Double latitude;

    @Column(precision = 10, scale = 7)
    private Double longitude;

    @Column(name = "double_header", nullable = false)
    private Boolean doubleHeader;

    @Builder
    public Game(Team awayTeam, Team homeTeam, GameStatus status, Integer awayScore,
                Integer homeScore, LocalDateTime dateTime, GameResult result,
                String stadium, Double latitude, Double longitude, Boolean doubleHeader) {
        this.awayTeam = awayTeam;
        this.homeTeam = homeTeam;
        this.status = status;
        this.awayScore = awayScore;
        this.homeScore = homeScore;
        this.dateTime = dateTime;
        this.result = result;
        this.stadium = stadium;
        this.latitude = latitude;
        this.longitude = longitude;
        this.doubleHeader = doubleHeader != null ? doubleHeader : false;
    }

    /**
     * 경기 상태 업데이트
     */
    public void updateStatus(GameStatus status) {
        this.status = status;
    }

    /**
     * 경기 결과 업데이트
     */
    public void updateResult(GameResult result, Integer homeScore, Integer awayScore) {
        this.result = result;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }

    /**
     * 특정 팀이 참여하는 경기인지 확인
     */
    public boolean isTeamGame(Long teamId) {
        return homeTeam.getId().equals(teamId) || awayTeam.getId().equals(teamId);
    }

    /**
     * 특정 팀이 홈팀인지 확인
     */
    public boolean isHomeTeam(Long teamId) {
        return homeTeam.getId().equals(teamId);
    }

    /**
     * 특정 팀이 어웨이팀인지 확인
     */
    public boolean isAwayTeam(Long teamId) {
        return awayTeam.getId().equals(teamId);
    }
}

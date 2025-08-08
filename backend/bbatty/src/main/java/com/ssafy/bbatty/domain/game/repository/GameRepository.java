package com.ssafy.bbatty.domain.game.repository;

import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.global.constants.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 경기 정보 Repository
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    
    /**
     * 특정 팀의 당일 경기 조회 (직관 인증용)
     */
    @Query("SELECT g FROM Game g WHERE (g.homeTeam.id = :teamId OR g.awayTeam.id = :teamId) " +
           "AND DATE(g.dateTime) = :date AND g.status = :status")
    List<Game> findTeamGamesToday(@Param("teamId") Long teamId, 
                                  @Param("date") LocalDate date, 
                                  @Param("status") GameStatus status);

    /**
     * 특정 기간 동안의 경기 조회 (3주간 일정 조회용)
     */
    @Query("SELECT g FROM Game g WHERE g.dateTime BETWEEN :startDate AND :endDate ORDER BY g.dateTime ASC")
    List<Game> findByDateTimeBetween(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
}

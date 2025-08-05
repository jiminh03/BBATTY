package com.ssafy.bbatty.domain.game.repository;

import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.global.constants.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}

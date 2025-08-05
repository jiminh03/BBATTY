package com.ssafy.schedule.repository;

import com.ssafy.schedule.entity.Game;
import com.ssafy.schedule.entity.Team;
import com.ssafy.schedule.common.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    
    @Query("SELECT g FROM Game g WHERE g.homeTeam = :team OR g.awayTeam = :team")
    List<Game> findByTeam(@Param("team") Team team);
    
    Optional<Game> findByHomeTeamAndAwayTeamAndDateTime(Team homeTeam, Team awayTeam, LocalDateTime dateTime);
    
    boolean existsByHomeTeamAndAwayTeamAndDateTime(Team homeTeam, Team awayTeam, LocalDateTime dateTime);
    
    @Query("SELECT g FROM Game g WHERE g.homeTeam = :homeTeam AND g.awayTeam = :awayTeam AND DATE(g.dateTime) = DATE(:gameDate)")
    Optional<Game> findByHomeTeamAndAwayTeamAndDate(@Param("homeTeam") Team homeTeam, @Param("awayTeam") Team awayTeam, @Param("gameDate") LocalDateTime gameDate);
    
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM Game g WHERE g.homeTeam = :homeTeam AND g.awayTeam = :awayTeam AND DATE(g.dateTime) = DATE(:gameDate)")
    boolean existsByHomeTeamAndAwayTeamAndDate(@Param("homeTeam") Team homeTeam, @Param("awayTeam") Team awayTeam, @Param("gameDate") LocalDateTime gameDate);
    
    List<Game> findByStatusAndDateTimeAfter(GameStatus status, LocalDateTime dateTime);
    
    @Query("SELECT g FROM Game g WHERE g.status = :status AND YEAR(g.dateTime) = :year ORDER BY g.dateTime ASC")
    List<Game> findByStatusAndYear(@Param("status") GameStatus status, @Param("year") int year);
}
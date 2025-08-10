package com.ssafy.schedule.global.repository;

import com.ssafy.schedule.global.entity.Game;
import com.ssafy.schedule.global.entity.Team;
import com.ssafy.schedule.global.constants.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    
    @Query("SELECT g FROM Game g WHERE g.homeTeam = :team OR g.awayTeam = :team")
    List<Game> findByTeam(@Param("team") Team team);
    
    Optional<Game> findByHomeTeamAndAwayTeamAndDateTime(Team homeTeam, Team awayTeam, LocalDateTime dateTime);
    
    boolean existsByHomeTeamAndAwayTeamAndDateTime(Team homeTeam, Team awayTeam, LocalDateTime dateTime);
    
    // 더블헤더 고려: 같은 날짜에 여러 경기가 있을 수 있으므로 첫 번째 경기만 반환
    @Query("SELECT g FROM Game g WHERE g.homeTeam = :homeTeam AND g.awayTeam = :awayTeam AND DATE(g.dateTime) = DATE(:gameDate) ORDER BY g.dateTime ASC")
    Optional<Game> findByHomeTeamAndAwayTeamAndDate(@Param("homeTeam") Team homeTeam, @Param("awayTeam") Team awayTeam, @Param("gameDate") LocalDateTime gameDate);
    
    // 더블헤더 고려: 같은 날짜에 해당 팀들의 경기가 있는지 확인 (시간 무관)
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM Game g WHERE g.homeTeam = :homeTeam AND g.awayTeam = :awayTeam AND DATE(g.dateTime) = DATE(:gameDate)")
    boolean existsByHomeTeamAndAwayTeamAndDate(@Param("homeTeam") Team homeTeam, @Param("awayTeam") Team awayTeam, @Param("gameDate") LocalDateTime gameDate);

    List<Game> findByStatusAndDateTimeAfter(GameStatus status, LocalDateTime dateTime);
    
    @Query("SELECT g FROM Game g WHERE g.status = :status AND YEAR(g.dateTime) = :year ORDER BY g.dateTime ASC")
    List<Game> findByStatusAndYear(@Param("status") GameStatus status, @Param("year") int year);
    
    @Query("SELECT g FROM Game g WHERE g.status = :status AND YEAR(g.dateTime) = :year AND DATE(g.dateTime) <= :endDate ORDER BY g.dateTime ASC")
    List<Game> findByStatusAndYearAndGameDateLessThanEqual(@Param("status") GameStatus status, @Param("year") int year, @Param("endDate") LocalDate endDate);
}
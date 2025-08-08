package com.ssafy.schedule.global.repository;

import com.ssafy.schedule.global.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    Optional<Team> findByName(String name);
    
    boolean existsByName(String name);
}
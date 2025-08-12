package com.ssafy.schedule.domain.user.repository;

import com.ssafy.schedule.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u.teamId FROM User u WHERE u.id = :userId")
    Optional<Long> findTeamIdByUserId(@Param("userId") Long userId);
}
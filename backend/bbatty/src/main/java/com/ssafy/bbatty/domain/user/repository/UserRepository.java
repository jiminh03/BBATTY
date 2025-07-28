package com.ssafy.bbatty.domain.user.repository;

import com.ssafy.bbatty.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 닉네임 중복 확인 - JPA 메서드 네이밍
    boolean existsByNickname(String nickname);

    // 복잡한 JOIN 쿼리는 @Query 유지
    @Query("SELECT u FROM User u JOIN FETCH u.team WHERE u.id = :userId")
    Optional<User> findByIdWithTeam(@Param("userId") Long userId);

    @Query("SELECT u FROM User u JOIN FETCH u.team JOIN FETCH u.userInfo WHERE u.id = :userId")
    Optional<User> findByIdWithTeamAndUserInfo(@Param("userId") Long userId);
}

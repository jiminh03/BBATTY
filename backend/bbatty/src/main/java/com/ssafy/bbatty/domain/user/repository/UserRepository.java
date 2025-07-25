package com.ssafy.bbatty.domain.user.repository;

import com.ssafy.bbatty.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 1. 닉네임 중복 확인 (GET /api/auth/check-nickname)
    boolean existsByNickname(String nickname);

    // 2. 사용자 ID로 기본 정보 조회 (토큰 검증, 프로필 조회 등)
    @Query("SELECT u FROM User u JOIN FETCH u.team WHERE u.id = :userId")
    Optional<User> findByIdWithTeam(@Param("userId") Long userId);

    // 3. 사용자 ID로 상세 정보 조회 (본인 정보 조회용)
    @Query("SELECT u FROM User u JOIN FETCH u.team JOIN FETCH u.userInfo WHERE u.id = :userId")
    Optional<User> findByIdWithTeamAndUserInfo(@Param("userId") Long userId);
}

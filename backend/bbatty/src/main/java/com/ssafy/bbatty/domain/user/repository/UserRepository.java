package com.ssafy.bbatty.domain.user.repository;

import com.ssafy.bbatty.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // === 닉네임 관련 ===
    /**
     * 닉네임 중복 확인 (회원가입, 닉네임 변경 시 사용)
     */
    boolean existsByNickname(String nickname);

    /**
     * 닉네임으로 사용자 조회
     */
    Optional<User> findByNickname(String nickname);
}

package com.ssafy.bbatty.domain.user.repository;

import com.ssafy.bbatty.domain.user.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo,Long> {

    // 카카오 ID로 조회 (기본) - JPA 메서드 네이밍
    Optional<UserInfo> findByKakaoId(String kakaoId);

    // 카카오 ID 존재 여부 확인 - JPA 메서드 네이밍
    boolean existsByKakaoId(String kakaoId);

    // 복잡한 JOIN 쿼리는 @Query 유지
    @Query("SELECT ui FROM UserInfo ui JOIN FETCH ui.user u JOIN FETCH u.team WHERE ui.kakaoId = :kakaoId")
    Optional<UserInfo> findByKakaoIdWithUserAndTeam(@Param("kakaoId") String kakaoId);
}

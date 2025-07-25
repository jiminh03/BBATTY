package com.ssafy.bbatty.domain.user.repository;

import com.ssafy.bbatty.domain.user.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo,Long> {

    // 1. 카카오 로그인 검증 (POST /api/auth/kakao)
    @Query("SELECT ui FROM UserInfo ui JOIN FETCH ui.user u JOIN FETCH u.team WHERE ui.kakaoId = :kakaoId")
    Optional<UserInfo> findByKakaoIdWithUserAndTeam(@Param("kakaoId") String kakaoId);

    // 2. 카카오 ID 존재 여부 확인 (회원가입 시 중복 체크)
    boolean existsByKakaoId(String kakaoId);
}

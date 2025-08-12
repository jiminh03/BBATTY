package com.ssafy.bbatty.domain.user.repository;

import com.ssafy.bbatty.domain.user.entity.UserInfo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserInfoRepository extends JpaRepository<UserInfo,Long> {

    // === 카카오 로그인/회원가입 관련 ===
    /**
     * 카카오 ID로 기존 회원 확인 (로그인 vs 회원가입 분기용)
     */
    boolean existsByKakaoId(String kakaoId);

    /**
     * 카카오 ID로 UserInfo 조회 (기본 조회)
     */
    Optional<UserInfo> findByKakaoId(String kakaoId);

    /**
     * 로그인 시 사용: 카카오 ID로 UserInfo + User + Team 모두 함께 조회
     */
    @EntityGraph(attributePaths = {"user", "user.team"})
    Optional<UserInfo> findWithUserAndTeamByKakaoId(String kakaoId);

    // === 기타 ===
    /**
     * 사용자 ID로 UserInfo 조회 (회원탈퇴 시 사용)
     */
    Optional<UserInfo> findByUserId(Long userId);
    
    /**
     * 회원 탈퇴 시 개인정보 하드 삭제
     */
    void deleteByUserId(Long userId);
}

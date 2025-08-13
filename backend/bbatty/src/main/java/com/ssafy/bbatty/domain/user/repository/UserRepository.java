package com.ssafy.bbatty.domain.user.repository;

import com.ssafy.bbatty.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // === 닉네임 관련 ===
    /**
     * 닉네임 중복 확인 (회원가입, 닉네임 변경 시 사용) - 삭제되지 않은 사용자만
     */
    boolean existsByNicknameAndIsDeletedFalse(String nickname);
    
    /**
     * 기존 닉네임 중복 확인 (하위 호환용)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.nickname = :nickname AND u.isDeleted = false")
    boolean existsByNickname(@Param("nickname") String nickname);

    /**
     * 닉네임으로 사용자 조회 - 삭제되지 않은 사용자만
     */
    Optional<User> findByNicknameAndIsDeletedFalse(String nickname);
    
    /**
     * 기존 닉네임 조회 (하위 호환용)
     */
    @Query("SELECT u FROM User u WHERE u.nickname = :nickname AND u.isDeleted = false")
    Optional<User> findByNickname(@Param("nickname") String nickname);
    
    /**
     * 사용자 ID 목록으로 사용자와 팀 정보 일괄 조회 (JOIN FETCH 사용)
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.team WHERE u.id IN :ids")
    List<User> findUsersWithTeamByIds(@Param("ids") List<Long> ids);
}

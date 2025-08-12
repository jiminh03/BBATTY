package com.ssafy.bbatty.domain.attendance.repository;

import com.ssafy.bbatty.domain.attendance.entity.UserAttended;
import com.ssafy.bbatty.domain.attendance.entity.UserAttendedId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 사용자 직관 기록 Repository
 */
@Repository
public interface UserAttendedRepository extends JpaRepository<UserAttended, UserAttendedId> {
    
    /**
     * 특정 사용자의 특정 경기 직관 인증 여부 확인
     * 중복 인증 방지용
     */
    boolean existsByUserIdAndGameId(Long userId, Long gameId);
    
    /**
     * 회원 탈퇴 시 사용자의 모든 직관 기록 하드 삭제
     */
    void deleteByUserId(Long userId);
}

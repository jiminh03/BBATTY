package com.ssafy.bbatty.domain.team.repository;

import com.ssafy.bbatty.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    // 1. 팀 선택 API용 - 순위별 팀 목록 조회 (POST /api/auth/select-team)
    List<Team> findAllByOrderByRankAsc();

    // 2. 팀 존재 여부 확인 (회원가입 시 유효성 검사)
    boolean existsById(Long teamId);
}

package com.ssafy.bbatty.domain.team.repository;

import com.ssafy.bbatty.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * 모든 팀 목록 조회 (회원가입 시 팀 선택용)
     */
    List<Team> findAllByOrderById();
}

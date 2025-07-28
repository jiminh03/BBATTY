package com.ssafy.bbatty.domain.board.repository;

import com.ssafy.bbatty.domain.board.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 메서드 쿼리를 이용한 커서 기반 페이징 (ID 기준, 생성순 정렬)
    Page<Post> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    // 첫 페이지 조회 (전체 조회, 생성순 정렬)
    Page<Post> findAllByOrderByIdDesc(Pageable pageable);

    // 팀별 게시글 조회 - 첫 페이지 (생성순 정렬)
    Page<Post> findByTeamIdOrderByIdDesc(Long teamId, Pageable pageable);

    // 팀별 게시글 조회 - 커서 기반 페이징 (생성순 정렬)
    Page<Post> findByTeamIdAndIdLessThanOrderByIdDesc(Long teamId, Long cursor, Pageable pageable);

    // 사용자별 게시글 조회 - 첫 페이지 (생성순 정렬)
    Page<Post> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    // 사용자별 게시글 조회 - 커서 기반 페이징 (생성순 정렬)
    Page<Post> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);
}
package com.ssafy.bbatty.domain.board.repository;

import com.ssafy.bbatty.domain.board.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 메서드 쿼리를 이용한 커서 기반 페이징 (ID 기준, 생성순 정렬) - 삭제되지 않은 게시글만
    Page<Post> findByIsDeletedFalseAndIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    // 첫 페이지 조회 (전체 조회, 생성순 정렬) - 삭제되지 않은 게시글만
    Page<Post> findByIsDeletedFalseOrderByIdDesc(Pageable pageable);

    // 팀별 게시글 조회 - 첫 페이지 (생성순 정렬) - 삭제되지 않은 게시글만
    Page<Post> findByIsDeletedFalseAndTeamIdOrderByIdDesc(Long teamId, Pageable pageable);

    // 팀별 게시글 조회 - 커서 기반 페이징 (생성순 정렬) - 삭제되지 않은 게시글만
    Page<Post> findByIsDeletedFalseAndTeamIdAndIdLessThanOrderByIdDesc(Long teamId, Long cursor, Pageable pageable);

    // 사용자별 게시글 조회 - 첫 페이지 (생성순 정렬) - 삭제되지 않은 게시글만
    Page<Post> findByIsDeletedFalseAndUserIdOrderByIdDesc(Long userId, Pageable pageable);

    // 사용자별 게시글 조회 - 커서 기반 페이징 (생성순 정렬) - 삭제되지 않은 게시글만
    Page<Post> findByIsDeletedFalseAndUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    // ===== 검색 기능 =====
    
    // 팀별 게시글 제목 검색 - 첫 페이지 (FULLTEXT 인덱스 활용)
    @Query(value = "SELECT * FROM post p WHERE p.team_id = :teamId AND p.is_deleted = false " +
           "AND MATCH(p.title) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
           "ORDER BY MATCH(p.title) AGAINST(:keyword IN NATURAL LANGUAGE MODE) DESC, p.id DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM post p WHERE p.team_id = :teamId AND p.is_deleted = false " +
                       "AND MATCH(p.title) AGAINST(:keyword IN NATURAL LANGUAGE MODE)")
    Page<Post> findByTeamIdAndTitleSearchOrderByIdDesc(
        @Param("teamId") Long teamId, 
        @Param("keyword") String keyword, 
        Pageable pageable);

    // 팀별 게시글 제목 검색 - 커서 기반 페이징 (FULLTEXT 인덱스 활용)
    @Query(value = "SELECT * FROM post p WHERE p.team_id = :teamId AND p.is_deleted = false " +
           "AND p.id < :cursor AND MATCH(p.title) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
           "ORDER BY p.id DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM post p WHERE p.team_id = :teamId AND p.is_deleted = false " +
                       "AND p.id < :cursor AND MATCH(p.title) AGAINST(:keyword IN NATURAL LANGUAGE MODE)")
    Page<Post> findByTeamIdAndTitleSearchAndIdLessThanOrderByIdDesc(
        @Param("teamId") Long teamId, 
        @Param("keyword") String keyword, 
        @Param("cursor") Long cursor, 
        Pageable pageable);
        
    // FULLTEXT가 안 될 경우를 대비한 LIKE 검색 방식 (백업용)
    @Query(value = "SELECT * FROM post p WHERE p.team_id = :teamId AND p.is_deleted = false " +
           "AND p.title LIKE CONCAT('%', :keyword, '%') " +
           "ORDER BY p.id DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM post p WHERE p.team_id = :teamId AND p.is_deleted = false " +
                       "AND p.title LIKE CONCAT('%', :keyword, '%')")
    Page<Post> findByTeamIdAndTitleLikeSearchOrderByIdDesc(
        @Param("teamId") Long teamId, 
        @Param("keyword") String keyword, 
        Pageable pageable);

    // FULLTEXT가 안 될 경우를 대비한 LIKE 검색 방식 - 커서 기반 (백업용)
    @Query(value = "SELECT * FROM post p WHERE p.team_id = :teamId AND p.is_deleted = false " +
           "AND p.id < :cursor AND p.title LIKE CONCAT('%', :keyword, '%') " +
           "ORDER BY p.id DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM post p WHERE p.team_id = :teamId AND p.is_deleted = false " +
                       "AND p.id < :cursor AND p.title LIKE CONCAT('%', :keyword, '%')")
    Page<Post> findByTeamIdAndTitleLikeSearchAndIdLessThanOrderByIdDesc(
        @Param("teamId") Long teamId, 
        @Param("keyword") String keyword, 
        @Param("cursor") Long cursor, 
        Pageable pageable);
    
}
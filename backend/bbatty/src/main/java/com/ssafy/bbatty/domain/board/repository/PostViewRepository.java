package com.ssafy.bbatty.domain.board.repository;

import com.ssafy.bbatty.domain.board.entity.PostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {
    
    @Query("SELECT COUNT(pv) FROM PostView pv WHERE pv.post.id = :postId")
    Integer countByPostId(@Param("postId") Long postId);
}
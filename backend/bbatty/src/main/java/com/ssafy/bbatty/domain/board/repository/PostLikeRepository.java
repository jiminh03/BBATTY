package com.ssafy.bbatty.domain.board.repository;

import com.ssafy.bbatty.domain.board.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId AND pl.likeAction = 'LIKE'")
    Long countLikesByPostId(@Param("postId") Long postId);
    
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId AND pl.likeAction = 'UNLIKE'")
    Long countUnlikesByPostId(@Param("postId") Long postId);


}
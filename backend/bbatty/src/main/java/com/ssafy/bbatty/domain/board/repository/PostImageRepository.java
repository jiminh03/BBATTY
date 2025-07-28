package com.ssafy.bbatty.domain.board.repository;

import com.ssafy.bbatty.domain.board.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    
    List<PostImage> findByImageUrlIn(List<String> imageUrls);
    
    @Query("SELECT pi FROM PostImage pi WHERE pi.status = 'UPLOADED' AND pi.imageUrl IN :imageUrls")
    List<PostImage> findUploadedImagesByUrls(@Param("imageUrls") List<String> imageUrls);
    
    @Query("SELECT pi FROM PostImage pi WHERE pi.status = 'UPLOADED' AND pi.post IS NULL")
    List<PostImage> findAllOrphanImages();
    
    List<PostImage> findByPostId(Long postId);
}
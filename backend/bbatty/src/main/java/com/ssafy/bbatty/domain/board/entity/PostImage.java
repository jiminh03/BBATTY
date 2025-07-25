package com.ssafy.bbatty.domain.board.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "post_image")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = true)
    private Post post;
    
    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ImageStatus status = ImageStatus.UPLOADED;
    
    public enum ImageStatus {
        UPLOADED,   // 업로드됨
        USED,       // 사용됨
        DELETED     // 삭제됨
    }
    
    public void markAsUsed() {
        this.status = ImageStatus.USED;
    }
    
    public void markAsDeleted() {
        this.status = ImageStatus.DELETED;
    }
    
    public boolean isOrphan() {
        return this.status == ImageStatus.UPLOADED;
    }
}
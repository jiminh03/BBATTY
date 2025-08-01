package com.ssafy.bbatty.domain.board.entity;

import com.ssafy.bbatty.domain.board.common.LikeAction;
import com.ssafy.bbatty.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_like")
@Getter
@Setter
public class PostLike {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // LIKE 또는 UNLIKE
    @Enumerated(EnumType.STRING)
    @Column(name = "like_action", nullable = false)
    private LikeAction likeAction;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public PostLike() {}
    
    public PostLike(User user, Post post, LikeAction likeAction) {
        this.user = user;
        this.post = post;
        this.likeAction = likeAction;
    }
}
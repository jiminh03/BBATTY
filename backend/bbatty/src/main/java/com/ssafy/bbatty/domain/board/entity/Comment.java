package com.ssafy.bbatty.domain.board.entity;

import com.ssafy.bbatty.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 데이터베이스와 매핑 된다는 의미의 어노테이션
@Entity
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    // 일반 Column과 다르게 객체에 주로 쓰임
    @JoinColumn(name="post_id", nullable = false)
    private Post post;
    // 다대일 관계를 맺고 있으며 즉시 불러오지 않고 사용할 떼 불러옴
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 댓글 내용
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 댓글 깊이 (0: 댓글, 1: 대댓글)
    @Column(nullable = false)
    private int depth = 0;

    // 부모 댓글 연관관계 (자기 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 삭제 여부 (소프트 삭제)
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 생성일시
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 수정일시
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 기본 생성자
    public Comment() {}

    // 댓글 생성용 생성자
    public Comment(Post post, User user, String content) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.depth = 0;
        this.parent = null;
        this.isDeleted = false;
    }

    // 대댓글 생성용 생성자
    public Comment(Post post, User user, String content, Comment parent) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.depth = 1;
        this.parent = parent;
        this.isDeleted = false;
    }
}

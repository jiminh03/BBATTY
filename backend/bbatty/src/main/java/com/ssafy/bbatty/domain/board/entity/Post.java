package com.ssafy.bbatty.domain.board.entity;

import com.ssafy.bbatty.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post")
@Getter
@Setter
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "team_id", nullable = false)
    private Long teamId;
    
    @Column(name = "title", nullable = false, length = 100)
    private String title;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;
    
    @Column(name = "is_same_team", nullable = false)
    private Boolean isSameTeam = false;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public Post() {}

    public Post(User user, Long teamId, String title, String content, Boolean isSameTeam) {
        this.user = user;
        this.teamId = teamId;
        this.title = title;
        this.content = content;
        this.isSameTeam = isSameTeam;
        this.viewCount = 0;
    }
    
    public void updateTitle(String title) {
        this.title = title;
    }
    
    public void updateContent(String content) {
        this.content = content;
    }
    
    public void updateIsSameTeam(Boolean isSameTeam) {
        this.isSameTeam = isSameTeam;
    }
    
    public void increaseViewCount() {
        this.viewCount++;
    }

}
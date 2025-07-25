package com.ssafy.bbatty.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PostCreateRequest {
    
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;
    
    @NotBlank(message = "내용은 필수입니다.")
    private String content;
    
    @NotNull(message = "팀 ID는 필수입니다.")
    private Long teamId;
    
    private Boolean isSameTeam = false;
    
    public PostCreateRequest() {}
    
    public PostCreateRequest(String title, String content, Long teamId, Boolean isSameTeam) {
        this.title = title;
        this.content = content;
        this.teamId = teamId;
        this.isSameTeam = isSameTeam;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Long getTeamId() {
        return teamId;
    }
    
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }
    
    public Boolean getIsSameTeam() {
        return isSameTeam;
    }
    
    public void setIsSameTeam(Boolean isSameTeam) {
        this.isSameTeam = isSameTeam;
    }
}
package com.ssafy.bbatty.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PostUpdateRequest {
    
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;
    
    @NotBlank(message = "내용은 필수입니다.")
    private String content;
    
    private Boolean isSameTeam = false;
    
    public PostUpdateRequest() {}
    
    public PostUpdateRequest(String title, String content, Boolean isSameTeam) {
        this.title = title;
        this.content = content;
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
    
    public Boolean getIsSameTeam() {
        return isSameTeam;
    }
    
    public void setIsSameTeam(Boolean isSameTeam) {
        this.isSameTeam = isSameTeam;
    }
}
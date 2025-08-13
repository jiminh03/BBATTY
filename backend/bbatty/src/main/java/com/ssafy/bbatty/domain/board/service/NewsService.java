package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.response.NewsSummaryDto;

import java.util.List;

public interface NewsService {
    
    /**
     * 팀별 뉴스 요약 조회
     * @param teamId 팀 ID
     * @return 뉴스 요약 리스트
     */
    List<NewsSummaryDto> getNewsSummaryByTeam(Long teamId);
}
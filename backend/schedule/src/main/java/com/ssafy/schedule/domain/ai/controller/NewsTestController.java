package com.ssafy.schedule.domain.ai.controller;

import com.ssafy.schedule.domain.ai.dto.NaverNewsResponse;
import com.ssafy.schedule.domain.ai.dto.NewsList;
import com.ssafy.schedule.domain.ai.dto.NewsSummaryResponse;
import com.ssafy.schedule.domain.ai.service.NaverNewsService;
import com.ssafy.schedule.domain.ai.service.NewSummaryService;
import com.ssafy.schedule.global.util.DateFilterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsTestController {

    private final NaverNewsService naverNewsService;
    private final NewSummaryService newSummaryService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NewsList> getNews(@RequestParam String query) {
        log.info("뉴스 검색 요청: {}", query);
        NaverNewsResponse news = naverNewsService.searchNews(query, 25, 1, "date");
        

        // 7일 이내 기사만 필터링하여 텍스트로 만들기
        StringBuilder allArticlesText = new StringBuilder();
        int validNewsCount = 0;
        for (NaverNewsResponse.Item newsItem : news.getItems()) {
            // 7일 이내 기사만 포함
            if (DateFilterUtil.isWithinSevenDays(newsItem.getPubDate())) {
                allArticlesText.append("제목: ").append(newsItem.getTitle()).append("\n");
                allArticlesText.append("내용: ").append(newsItem.getDescription()).append("\n");
                allArticlesText.append("발행일: ").append(newsItem.getPubDate()).append("\n");
                allArticlesText.append("---\n");
                validNewsCount++;
            }
        }
        
        log.info("전체 기사 {}개 중 7일 이내 기사 {}개 필터링", news.getItems().size(), validNewsCount);
        
        if (validNewsCount == 0) {
            log.info("7일 이내 기사가 없습니다.");
            return ResponseEntity.ok(new NewsList());
        }

        try {
            // GPT에 20개 기사를 모두 보내서 한화 이글즈 관련 5개만 선별 요청
            NewsList newsList =  newSummaryService.summarize(allArticlesText.toString(), "한화 이글즈");
            return ResponseEntity.ok(newsList);

        } catch (Exception e) {
            log.error("뉴스 요약 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("뉴스 요약 처리 실패", e);
        }
    }

    private List<NewsSummaryResponse> parseGptResponse(String gptResponse) {
        List<NewsSummaryResponse> summaries = new ArrayList<>();
        String[] sections = gptResponse.split("\n\n");
        
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;
            
            String[] lines = section.split("\n");
            if (lines.length >= 2) {
                NewsSummaryResponse summary = new NewsSummaryResponse();
                summary.setTitle(lines[0].trim());
                
                StringBuilder content = new StringBuilder();
                for (int i = 1; i < lines.length; i++) {
                    if (!lines[i].trim().isEmpty()) {
                        content.append(lines[i].trim()).append(" ");
                    }
                }
                summary.setContent(content.toString().trim());
                summaries.add(summary);
            }
        }
        
        return summaries;
    }

}

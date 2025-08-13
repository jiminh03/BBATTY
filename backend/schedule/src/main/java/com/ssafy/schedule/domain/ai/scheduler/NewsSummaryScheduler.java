package com.ssafy.schedule.domain.ai.scheduler;

import com.ssafy.schedule.domain.ai.dto.NaverNewsResponse;
import com.ssafy.schedule.domain.ai.dto.NewsList;
import com.ssafy.schedule.domain.ai.service.NaverNewsService;
import com.ssafy.schedule.domain.ai.service.NewSummaryService;
import com.ssafy.schedule.global.constants.RedisKey;
import com.ssafy.schedule.global.entity.Team;
import com.ssafy.schedule.global.repository.TeamRepository;
import com.ssafy.schedule.global.util.DateFilterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsSummaryScheduler {

    private final NaverNewsService naverNewsService;
    private final NewSummaryService newSummaryService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TeamRepository teamRepository;

    /**
     * 서버 시작 시 조건부 초기화 - Redis에 데이터가 없는 팀만 처리
     */
    @PostConstruct
    public void initializeNewsSummaryIfNeeded() {
        log.info("서버 시작 시 뉴스 요약 조건부 초기화 시작");
        
        List<Team> teams = teamRepository.findAll();
        int initializedCount = 0;
        
        for (Team team : teams) {
            String redisKey = RedisKey.NEWS_SUMMARY + team.getId();
            
            // Redis에 해당 팀 데이터가 없는 경우에만 처리
            if (!redisTemplate.hasKey(redisKey)) {
                log.info("{} 데이터가 없어 초기화 진행", team.getName());
                try {
                    processTeamNewsSummary(team);
                    initializedCount++;
                } catch (Exception e) {
                    log.error("{} 초기화 중 오류 발생: {}", team.getName(), e.getMessage(), e);
                }
            } else {
                log.debug("{} 데이터 이미 존재", team.getName());
            }
        }
        
        log.info("뉴스 요약 조건부 초기화 완료 - {} / {} 팀 초기화됨", initializedCount, teams.size());
    }

    /**
     * 뉴스 요약 스케줄러 - 매일 6시, 12시, 18시 실행
     */
    @Scheduled(cron = "0 0 6,12,18 * * *")
    public void generateNewsSummary() {
        log.info("뉴스 요약 스케줄러 시작");
        
        List<Team> teams = teamRepository.findAll();
        for (Team team : teams) {
            try {
                processTeamNewsSummary(team);
            } catch (Exception e) {
                log.error("{} 뉴스 요약 처리 중 오류 발생: {}", team.getName(), e.getMessage(), e);
            }
        }
        
        log.info("뉴스 요약 스케줄러 완료");
    }

    /**
     * 개별 팀의 뉴스 요약을 처리하는 메서드 (재시도 로직 포함)
     */
    private void processTeamNewsSummary(Team team) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 실제 뉴스 처리 로직 실행
                processTeamNewsSummaryInternal(team);
                log.info("{} (ID: {}) 뉴스 요약 처리 성공", team.getName(), team.getId());
                return; // 성공시 메서드 종료
                
            } catch (Exception e) {
                retryCount++;
                log.warn("{} (ID: {}) 뉴스 요약 처리 실패 (시도: {}/{}) - 오류: {}", 
                    team.getName(), team.getId(), retryCount, maxRetries, e.getMessage());
                
                if (retryCount >= maxRetries) {
                    log.error("{} (ID: {}) 뉴스 요약 처리 최종 실패 - 최대 재시도 횟수 초과", 
                        team.getName(), team.getId(), e);
                    throw new RuntimeException("뉴스 요약 처리 최종 실패", e);
                }
                
                // 재시도 전 대기 (지수 백오프: 1초, 2초, 4초)
                try {
                    long waitTime = (long) Math.pow(2, retryCount - 1) * 1000;
                    log.info("{} (ID: {}) {} 초 후 재시도...", team.getName(), team.getId(), waitTime / 1000);
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 대기 중 중단됨", ie);
                }
            }
        }
    }

    /**
     * 실제 뉴스 요약 처리 로직
     */
    private void processTeamNewsSummaryInternal(Team team) {
        // 1. 뉴스 검색 (팀 이름으로 검색)
        NaverNewsResponse news = naverNewsService.searchNews(team.getName(), 25, 1, "date");
        if (news == null || news.getItems() == null) {
            throw new RuntimeException("네이버 뉴스 API 응답이 null입니다");
        }
        
        // 2. 7일 이내 기사 필터링
        StringBuilder allArticlesText = new StringBuilder();
        int validNewsCount = 0;
        
        for (NaverNewsResponse.Item newsItem : news.getItems()) {
            if (DateFilterUtil.isWithinSevenDays(newsItem.getPubDate())) {
                allArticlesText.append("제목: ").append(newsItem.getTitle()).append("\n");
                allArticlesText.append("내용: ").append(newsItem.getDescription()).append("\n");
                allArticlesText.append("발행일: ").append(newsItem.getPubDate()).append("\n");
                allArticlesText.append("---\n");
                validNewsCount++;
            }
        }
        
        if (validNewsCount == 0) {
            log.warn("{} - 7일 이내 기사가 없습니다.", team.getName());
            return;
        }
        
        // 3. AI 요약 생성 (팀 이름으로 요약)
        NewsList newsList = newSummaryService.summarize(allArticlesText.toString(), team.getName());
        if (newsList == null || newsList.getItems() == null || newsList.getItems().isEmpty()) {
            throw new RuntimeException("AI 요약 생성 실패 - 결과가 비어있습니다");
        }
        
        // 4. Redis Hash에 저장 (팀 ID를 키로 사용, 6시간 TTL)
        String redisKey = RedisKey.NEWS_SUMMARY + team.getId();
        
        // 기존 데이터 삭제
        redisTemplate.delete(redisKey);
        
        // 각 뉴스 아이템을 Hash로 저장
        if (newsList.getItems() != null && !newsList.getItems().isEmpty()) {
            for (int i = 0; i < newsList.getItems().size(); i++) {
                var newsItem = newsList.getItems().get(i);
                String hashKey = "news_" + i;
                redisTemplate.opsForHash().put(redisKey, hashKey + ":title", newsItem.getTitle());
                redisTemplate.opsForHash().put(redisKey, hashKey + ":summary", newsItem.getSummary());
            }
        }
        redisTemplate.expire(redisKey, Duration.ofHours(6));
        
        // 저장 검증
        Long hashSize = redisTemplate.opsForHash().size(redisKey);
        if (hashSize == null || hashSize == 0) {
            throw new RuntimeException("Redis Hash 저장 실패 - 저장 후 조회되지 않음");
        }
        
        log.info("{} (ID: {}) 뉴스 요약 완료 - {} 개 기사 처리, {} 개 요약 생성", 
            team.getName(), team.getId(), validNewsCount, newsList.getItems().size());
    }
}
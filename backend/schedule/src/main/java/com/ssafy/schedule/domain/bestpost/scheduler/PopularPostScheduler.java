package com.ssafy.schedule.domain.bestpost.scheduler;

import com.ssafy.schedule.domain.bestpost.dto.PostEventDto;
import com.ssafy.schedule.domain.bestpost.kafka.PostEventKafkaConsumer;
import com.ssafy.schedule.domain.bestpost.service.PopularPostRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularPostScheduler {
    
    private final PostEventKafkaConsumer kafkaConsumer;
    private final PopularPostRankingService rankingService;
    
    // 점수 가중치
    private static final double VIEW_WEIGHT = 1.0;    // 조회수 1점
    private static final double LIKE_WEIGHT = 5.0;    // 좋아요 5점  
    private static final double COMMENT_WEIGHT = 10.0; // 댓글 10점
    
    /**
     * 조회수 이벤트 배치 처리 - 1분마다 실행 (우선 1분)
     */
    @Scheduled(fixedRate = 60000) // 10분 = 600,000ms
    public void processViewEvents() {
        ConcurrentLinkedQueue<PostEventDto> viewEvents = kafkaConsumer.getViewEvents();
        
        if (viewEvents.isEmpty()) {
            log.debug("처리할 조회수 이벤트가 없습니다.");
            return;
        }
        
        // 팀별, 게시글별 조회수 집계
        Map<String, Integer> viewCounts = new HashMap<>(); // key: "teamId:postId", value: count
        
        PostEventDto event;
        int processedCount = 0;
        
        // 큐에서 모든 이벤트 꺼내서 집계
        while ((event = viewEvents.poll()) != null) {
            String key = event.getTeamId() + ":" + event.getPostId();
            viewCounts.put(key, viewCounts.getOrDefault(key, 0) + 1);
            processedCount++;
        }
        
        // Redis에 점수 업데이트
        for (Map.Entry<String, Integer> entry : viewCounts.entrySet()) {
            String[] parts = entry.getKey().split(":");
            Long teamId = Long.valueOf(parts[0]);
            Long postId = Long.valueOf(parts[1]);
            int viewCount = entry.getValue();

            double score = viewCount * VIEW_WEIGHT;
            rankingService.updatePostScore(teamId, postId, score);
        }
        
        log.info("조회수 이벤트 배치 처리 완료 - 처리된 이벤트: {}, 업데이트된 게시글: {}", 
            processedCount, viewCounts.size());
    }
    
    /**
     * 좋아요 이벤트 배치 처리 - 30분마다 실행(우선 2분)
     */
    @Scheduled(fixedRate = 60000*2) // 30분 = 1,800,000ms
    public void processLikeEvents() {
        ConcurrentLinkedQueue<PostEventDto> likeEvents = kafkaConsumer.getLikeEvents();
        
        if (likeEvents.isEmpty()) {
            log.debug("처리할 좋아요 이벤트가 없습니다.");
            return;
        }
        
        // 팀별, 게시글별 좋아요 집계
        Map<String, Integer> likeCounts = new HashMap<>();
        
        PostEventDto event;
        int processedCount = 0;
        
        while ((event = likeEvents.poll()) != null) {
            String key = event.getTeamId() + ":" + event.getPostId();
            likeCounts.put(key, likeCounts.getOrDefault(key, 0) + 1);
            processedCount++;
        }
        
        // Redis에 점수 업데이트
        for (Map.Entry<String, Integer> entry : likeCounts.entrySet()) {
            String[] parts = entry.getKey().split(":");
            Long teamId = Long.valueOf(parts[0]);
            Long postId = Long.valueOf(parts[1]);
            int likeCount = entry.getValue();
            
            double score = likeCount * LIKE_WEIGHT;
            log.info("{}에 {}를 처리 : {}", teamId, postId, score);
            rankingService.updatePostScore(teamId, postId, score);
        }
        
        log.info("좋아요 이벤트 배치 처리 완료 - 처리된 이벤트: {}, 업데이트된 게시글: {}", 
            processedCount, likeCounts.size());
    }
    
    /**
     * 댓글 이벤트 배치 처리 - 1시간마다 실행 (우선 3분)
     */
    @Scheduled(fixedRate = 60000*3) // 1시간 = 3,600,000ms
    public void processCommentEvents() {
        ConcurrentLinkedQueue<PostEventDto> commentEvents = kafkaConsumer.getCommentEvents();
        
        if (commentEvents.isEmpty()) {
            log.debug("처리할 댓글 이벤트가 없습니다.");
            return;
        }
        
        // 팀별, 게시글별 댓글 집계
        Map<String, Integer> commentCounts = new HashMap<>();
        
        PostEventDto event;
        int processedCount = 0;
        
        while ((event = commentEvents.poll()) != null) {
            String key = event.getTeamId() + ":" + event.getPostId();
            commentCounts.put(key, commentCounts.getOrDefault(key, 0) + 1);
            processedCount++;
        }
        
        // Redis에 점수 업데이트
        for (Map.Entry<String, Integer> entry : commentCounts.entrySet()) {
            String[] parts = entry.getKey().split(":");
            Long teamId = Long.valueOf(parts[0]);
            Long postId = Long.valueOf(parts[1]);
            int commentCount = entry.getValue();
            
            double score = commentCount * COMMENT_WEIGHT;
            rankingService.updatePostScore(teamId, postId, score);
        }
        
        log.info("댓글 이벤트 배치 처리 완료 - 처리된 이벤트: {}, 업데이트된 게시글: {}", 
            processedCount, commentCounts.size());
    }
    
    /**
     * 인기글 점수 감쇠 - 30분마다 실행
     */
    @Scheduled(fixedRate = 1800000) // 30분 = 1,800,000ms
    public void applyScoreDecay() {
        double decayRate = 0.80; //20% 감쇠
        rankingService.applyDecayToAllTeams(decayRate);
        
        log.info("인기글 점수 감쇠 적용 완료 - 감쇠율: {}%", (1 - decayRate) * 100);
    }
}
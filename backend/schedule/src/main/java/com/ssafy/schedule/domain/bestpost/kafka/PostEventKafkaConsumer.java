package com.ssafy.schedule.domain.bestpost.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.schedule.domain.bestpost.dto.PostEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventKafkaConsumer {

    private final ObjectMapper objectMapper;
    
    // 이벤트 타입별로 큐에 저장 (배치 처리를 위해)
    private final ConcurrentLinkedQueue<PostEventDto> viewEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PostEventDto> likeEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PostEventDto> commentEvents = new ConcurrentLinkedQueue<>();
    
    @KafkaListener(topics = "post-view-events", groupId = "schedule-service")
    public void consumeViewEvent(String message) {
        try {
            PostEventDto event = objectMapper.readValue(message, PostEventDto.class);
            viewEvents.offer(event);
            log.debug("조회수 이벤트 수신: postId={}, userId={}, teamId={}", 
                event.getPostId(), event.getUserId(), event.getTeamId());
        } catch (JsonProcessingException e) {
            log.error("조회수 이벤트 파싱 실패: {}", e.getMessage());
        }
    }
    
    @KafkaListener(topics = "post-like-events", groupId = "schedule-service")
    public void consumeLikeEvent(String message) {
        try {
            PostEventDto event = objectMapper.readValue(message, PostEventDto.class);
            likeEvents.offer(event);
            log.debug("좋아요 이벤트 수신: postId={}, userId={}, teamId={}", 
                event.getPostId(), event.getUserId(), event.getTeamId());
        } catch (JsonProcessingException e) {
            log.error("좋아요 이벤트 파싱 실패: {}", e.getMessage());
        }
    }
    
    @KafkaListener(topics = "post-comment-events", groupId = "schedule-service")
    public void consumeCommentEvent(String message) {
        try {
            PostEventDto event = objectMapper.readValue(message, PostEventDto.class);
            commentEvents.offer(event);
            log.debug("댓글 이벤트 수신: postId={}, userId={}, teamId={}, commentId={}", 
                event.getPostId(), event.getUserId(), event.getTeamId(), event.getCommentId());
        } catch (JsonProcessingException e) {
            log.error("댓글 이벤트 파싱 실패: {}", e.getMessage());
        }
    }
    
    // 배치 처리용 이벤트 큐 접근 메서드들
    public ConcurrentLinkedQueue<PostEventDto> getViewEvents() {
        return viewEvents;
    }
    
    public ConcurrentLinkedQueue<PostEventDto> getLikeEvents() {
        return likeEvents;
    }
    
    public ConcurrentLinkedQueue<PostEventDto> getCommentEvents() {
        return commentEvents;
    }
}
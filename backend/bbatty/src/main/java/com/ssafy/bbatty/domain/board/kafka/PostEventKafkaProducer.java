package com.ssafy.bbatty.domain.board.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.bbatty.domain.board.dto.event.PostEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 게시물 이벤트를 Kafka로 전송하는 Producer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventKafkaProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String POST_LIKE_TOPIC = "post-like-events";
    private static final String POST_VIEW_TOPIC = "post-view-events"; 
    private static final String POST_COMMENT_TOPIC = "post-comment-events";
    
    /**
     * 게시물 조회 이벤트를 Kafka로 전송
     */
    public void sendViewEvent(Long postId, Long userId, Long teamId) {
        PostEventDto eventDto = PostEventDto.createViewEvent(postId, userId, teamId);
        sendEvent(eventDto, POST_VIEW_TOPIC, "VIEW");
    }
    
    /**
     * 게시물 좋아요 이벤트를 Kafka로 전송
     */
    public void sendLikeEvent(Long postId, Long userId, Long teamId) {
        PostEventDto eventDto = PostEventDto.createLikeEvent(postId, userId, teamId);
        sendEvent(eventDto, POST_LIKE_TOPIC, "LIKE");
    }
    
    /**
     * 댓글 작성 이벤트를 Kafka로 전송
     */
    public void sendCommentEvent(Long postId, Long userId, Long teamId, Long commentId) {
        PostEventDto eventDto = PostEventDto.createCommentEvent(postId, userId, teamId, commentId);
        sendEvent(eventDto, POST_COMMENT_TOPIC, "COMMENT");
    }
    
    /**
     * 이벤트를 Kafka로 전송하는 공통 메서드
     */
    private void sendEvent(PostEventDto eventDto, String topic, String eventType) {
        try {
            String messageJson = objectMapper.writeValueAsString(eventDto);
            String key = String.valueOf(eventDto.getTeamId());
            
            log.debug("게시물 {} 이벤트 Kafka 전송: postId={}, userId={}, topic={}", eventType, eventDto.getPostId(), eventDto.getUserId(), topic);
            
            kafkaTemplate.send(topic, key, messageJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("게시물 {} 이벤트 Kafka 전송 실패: postId={}, userId={}, topic={}", 
                                    eventType, eventDto.getPostId(), eventDto.getUserId(), topic, ex);
                        } else {
                            log.debug("게시물 {} 이벤트 Kafka 전송 성공: postId={}, userId={}, topic={}, offset={}", 
                                    eventType, eventDto.getPostId(), eventDto.getUserId(), topic, result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("게시물 {} 이벤트 직렬화 실패: postId={}, userId={}, topic={}", 
                    eventType, eventDto.getPostId(), eventDto.getUserId(), topic, e);
        }
    }
}
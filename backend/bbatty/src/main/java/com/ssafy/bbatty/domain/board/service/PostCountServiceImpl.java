package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.common.LikeAction;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.entity.PostLike;
import com.ssafy.bbatty.domain.board.entity.PostView;
import com.ssafy.bbatty.domain.board.kafka.PostEventKafkaProducer;
import com.ssafy.bbatty.domain.board.repository.CommentRepository;
import com.ssafy.bbatty.domain.board.repository.PostLikeRepository;
import com.ssafy.bbatty.domain.board.repository.PostRepository;
import com.ssafy.bbatty.domain.board.repository.PostViewRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostCountServiceImpl implements PostCountService {
    
    private final RedisUtil redisUtil;
    private final PostViewRepository postViewRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostEventKafkaProducer postEventKafkaProducer;
    
    private static final String VIEW_COUNT_KEY = "post:view:";
    private static final String LIKE_COUNT_KEY = "post:like:";
    private static final String COMMENT_COUNT_KEY = "post:comment:";
    
    @Override
    public Integer getViewCount(Long postId) {
        String key = VIEW_COUNT_KEY + postId;
        Integer count = redisUtil.getValue(key, Integer.class);
        
        if (count == null) {
            count = postViewRepository.countByPostId(postId);
            redisUtil.setValue(key, count);
        }
        
        return count;
    }
    
    @Override
    public Integer getLikeCount(Long postId) {
        String key = LIKE_COUNT_KEY + postId;
        Integer count = redisUtil.getValue(key, Integer.class);
        
        if (count == null) {
            Integer likeCount = postLikeRepository.countLikesByPostId(postId);
            Integer unlikeCount = postLikeRepository.countUnlikesByPostId(postId);
            count = likeCount - unlikeCount;
            redisUtil.setValue(key, count);
        }
        return count;
    }

    @Override
    public Integer getCommentCount(Long postId) {
        String key = COMMENT_COUNT_KEY + postId;
        Integer count = redisUtil.getValue(key, Integer.class);

        if (count == null) {
            count = commentRepository.countByPostId(postId);
            redisUtil.setValue(key, count);
        }

        return count;
    }
    
    @Override
    @Transactional
    public void incrementViewCount(Long postId, Long userId) {
        // 1. PostView 로그 테이블에 조회 기록 저장
        Post post = postRepository.findById(postId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        
        if (post != null && user != null) {
            PostView postView = new PostView(user, post);
            postViewRepository.save(postView);
        }

        String key = VIEW_COUNT_KEY + postId;
        if (redisUtil.hasKey(key)) {
            Integer currentCount = redisUtil.getValue(key, Integer.class);
            redisUtil.setValue(key, currentCount + 1);
        } else {
            Integer dbCount = postViewRepository.countByPostId(postId);
            redisUtil.setValue(key, dbCount);
        }
    }

    @Override
    @Transactional
    public void incrementLikeCount(Long postId, Long userId) {
        // 1. PostLike 테이블에 좋아요 기록 저장
        Post post = postRepository.findById(postId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (post != null && user != null) {
            PostLike postLike = new PostLike(user, post, LikeAction.LIKE);
            postLikeRepository.save(postLike);
            
            // 좋아요 이벤트를 Kafka로 전송 (실시간 처리)
            postEventKafkaProducer.sendLikeEvent(postId, userId, post.getTeamId());
        }

        // 2. Redis 좋아요 카운트 증가
        String key = LIKE_COUNT_KEY + postId;
        if (redisUtil.hasKey(key)) {
            Integer currentCount = redisUtil.getValue(key, Integer.class);
            redisUtil.setValue(key, currentCount + 1);
        } else {
            Integer likeCount = postLikeRepository.countLikesByPostId(postId);
            Integer unlikeCount = postLikeRepository.countUnlikesByPostId(postId);
            Integer dbCount = likeCount - unlikeCount;
            redisUtil.setValue(key, dbCount);
        }
    }
    
    @Override
    @Transactional
    public void decrementLikeCount(Long postId, Long userId) {
        // 1. PostLike 테이블에 좋아요 취소 기록 저장 (UNLIKE)
        Post post = postRepository.findById(postId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (post != null && user != null) {
            PostLike postLike = new PostLike(user, post, LikeAction.UNLIKE);
            postLikeRepository.save(postLike);
        }

        // 2. Redis 좋아요 카운트 감소
        String key = LIKE_COUNT_KEY + postId;
        if (redisUtil.hasKey(key)) {
            Integer currentCount = redisUtil.getValue(key, Integer.class);
            if (currentCount > 0) {
                redisUtil.setValue(key, currentCount - 1);
            }
        } else {
            Integer likeCount = postLikeRepository.countLikesByPostId(postId);
            Integer unlikeCount = postLikeRepository.countUnlikesByPostId(postId);
            Integer dbCount = likeCount - unlikeCount;
            redisUtil.setValue(key, dbCount > 0 ? dbCount - 1 : 0);
        }
    }
    
    @Override
    public void refreshCountsFromDB(Long postId) {
        String viewKey = VIEW_COUNT_KEY + postId;
        String likeKey = LIKE_COUNT_KEY + postId;
        String commentKey = COMMENT_COUNT_KEY + postId;
        
        Integer viewCount = postViewRepository.countByPostId(postId);
        Integer likes = postLikeRepository.countLikesByPostId(postId);
        Integer unlikes = postLikeRepository.countUnlikesByPostId(postId);
        Integer likeCount = likes - unlikes;
        Integer commentCount = commentRepository.countByPostId(postId);
        
        redisUtil.setValue(viewKey, viewCount);
        redisUtil.setValue(likeKey, likeCount);
        redisUtil.setValue(commentKey, commentCount);
    }

}
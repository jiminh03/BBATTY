package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.response.PostListPageResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostListResponse;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularPostServiceImpl implements PopularPostService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;
    private final PostCountService postCountService;
    
    private static final String POPULAR_POST_KEY_PREFIX = "popular_posts:team:";
    private static final int PAGE_SIZE = 5; // 한 번에 가져올 게시글 수
    
    @Override
    public PostListPageResponse getPopularPostsByTeam(Long teamId, Long cursor) {
        String key = POPULAR_POST_KEY_PREFIX + teamId;
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        
        // Redis에서 높은 점수순으로 모든 인기글 ID 조회
        Set<Object> allPopularPostIds = zSetOps.reverseRange(key, 0, -1);
        
        List<PostListResponse> popularPosts = new ArrayList<>();
        boolean hasNext = false;
        Long nextCursor = null;
        int count = 0;
        boolean foundCursor = (cursor == null); // cursor가 null이면 첫 페이지
        
        if (allPopularPostIds != null && !allPopularPostIds.isEmpty()) {
            for (Object postIdObj : allPopularPostIds) {
                try {
                    Long postId = Long.valueOf(postIdObj.toString());
                    
                    // 커서 기반 페이징 처리
                    if (!foundCursor) {
                        if (postId.equals(cursor)) {
                            foundCursor = true;
                        }
                        continue; // 커서를 찾을 때까지 건너뛰기
                    }
                    
                    // 페이지 크기만큼 데이터 수집
                    if (count < PAGE_SIZE) {
                        // 게시글 정보 조회
                        Post post = postRepository.findById(postId).orElse(null);
                        
                        // 삭제되지 않은 게시글만 포함
                        if (post != null && !post.getIsDeleted()) {
                            PostListResponse response = new PostListResponse(post);
                            
                            // 조회수, 좋아요, 댓글 수 설정
                            response.setViewCount(postCountService.getViewCount(post.getId()));
                            response.setLikeCount(postCountService.getLikeCount(post.getId()));
                            response.setCommentCount(postCountService.getCommentCount(post.getId()));
                            
                            popularPosts.add(response);
                            count++;
                        }
                    } else {
                        // 다음 페이지가 있는지 확인
                        hasNext = true;
                        break;
                    }
                } catch (NumberFormatException e) {
                    log.warn("인기글 ID 파싱 실패: {}", postIdObj, e);
                }
            }
            
            // nextCursor 설정
            if (hasNext && !popularPosts.isEmpty()) {
                nextCursor = popularPosts.get(popularPosts.size() - 1).getId();
            }
        }
        
        log.debug("팀 {} 인기글 페이징 조회 완료: {}개, hasNext: {}", teamId, popularPosts.size(), hasNext);
        return new PostListPageResponse(popularPosts, hasNext, nextCursor);
    }
}
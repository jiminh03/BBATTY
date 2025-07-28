package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostDetailResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostListPageResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostListResponse;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.repository.PostRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final RedisTemplate<String, String> postRedisTemplate;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostImageService postImageService;
    private static final String VIEW_COUNT_KEY_PREFIX = "view:count:";
    private static final String LIKE_COUNT_KEY_PREFIX = "like:count:";
    private static final int PAGE_SIZE = 5; // 한 번에 가져올 게시글 수

    /*
    게시물 생성 메소드
     */
    @Override
    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, Long userId) {
        User user = (User) userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        
        Post post = new Post(
                user,
                request.getTeamId(),
                request.getTitle(),
                request.getContent(),
                request.getIsSameTeam()
        );
        Post savedPost = postRepository.save(post);
        System.out.println(savedPost.getCreatedAt() + "[debug]");
        System.out.println(savedPost.getTitle() + "[debug]");
        postImageService.processImagesInContent(request.getContent(), savedPost);
        return new PostCreateResponse(savedPost.getId(), "게시글이 성공적으로 작성되었습니다.");
    }

    /*
    게시물 삭제 메서드
     */
    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // 작성자 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.POST_FORBIDDEN);
        }

        // 먼저 S3에서 이미지 삭제
        postImageService.deleteImagesForPost(postId);
        
        // 그 다음 게시글 삭제 (PostImage는 CASCADE로 자동 삭제됨)
        postRepository.delete(post);
    }

    /*
    커서기반 페이지네이션 게시물 조회 메서드 (ID 기준)
    */
    public PostListPageResponse getPostList(Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Post> postPage;

        if (cursor == null) {
            // 첫 페이지 - 전체 조회
            postPage = postRepository.findAllByOrderByIdDesc(pageable);
        } else {
            // 다음 페이지 - 커서 이후 데이터 조회
            postPage = postRepository.findByIdLessThanOrderByIdDesc(cursor, pageable);
        }

        List<PostListResponse> posts = postPage.getContent()
                .stream()
                .map(PostListResponse::new)
                .collect(Collectors.toList());

        boolean hasNext = postPage.hasNext();
        Long nextCursor = null;

        if (hasNext && !posts.isEmpty()) {
            nextCursor = posts.getLast().getId();
        }

        return new PostListPageResponse(posts, hasNext, nextCursor);
    }

    /*
    팀별 커서기반 페이지네이션 게시물 조회 메서드 (ID 기준)
    */
    @Override
    public PostListPageResponse getPostListByTeam(Long teamId, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Post> postPage;

        if (cursor == null) {
            // 첫 페이지 - 팀별 조회
            postPage = postRepository.findByTeamIdOrderByIdDesc(teamId, pageable);
        } else {
            // 다음 페이지 - 팀별 커서 이후 데이터 조회
            postPage = postRepository.findByTeamIdAndIdLessThanOrderByIdDesc(teamId, cursor, pageable);
        }

        List<PostListResponse> posts = postPage.getContent()
                .stream()
                .map(PostListResponse::new)
                .collect(Collectors.toList());

        boolean hasNext = postPage.hasNext();
        Long nextCursor = null;

        if (hasNext && !posts.isEmpty()) {
            nextCursor = posts.getLast().getId();
        }

        return new PostListPageResponse(posts, hasNext, nextCursor);
    }

    /*
    게시물 상세 조회 메서드
     */
    @Override
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        incrementViewCount(postId, post.getViewCount());

        return PostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .authorNickname(post.getUser().getNickname())
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt().toString())
                .updatedAt(post.getUpdatedAt().toString())
                .build();
    }

    /**
     * 게시글 조회수 증가 (Redis에만 저장)
     * @param postId 게시글 ID
     */
    public void incrementViewCount(Long postId, Long DBViewCount) {
        String key = VIEW_COUNT_KEY_PREFIX + postId;

        try {
            // Redis에서 조회수 증가
            Long newCount = postRedisTemplate.opsForValue().increment(key);
            System.out.println("newCount:" + newCount);
            // 처음 증가된 경우 (Redis에 키가 없어서 1이 된 경우)
            if (newCount == 1L) {
                System.out.println("들어옴?:" + postId);
                // DB의 현재 조회수를 가져와서 Redis에 설정한다.
                if (DBViewCount != null && DBViewCount > 0) {
                    // DB의 조회수 + 1 (현재 조회 포함)
                    postRedisTemplate.opsForValue().set(key, String.valueOf(DBViewCount + 1));
                }
            }


            System.out.println("Redis 조회수 증가 완료 - postId:" + postId);

        } catch (Exception e) {
            log.error("Redis 조회수 증가 실패 - postId: {}, error: {}", postId, e.getMessage());
        }
    }

    /*
    사용자별 커서기반 페이지네이션 게시물 조회 메서드 (ID 기준)
    */
    @Override
    public PostListPageResponse getPostListByUser(Long userId, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Post> postPage;

        if (cursor == null) {
            // 첫 페이지 - 사용자별 조회
            postPage = postRepository.findByUserIdOrderByIdDesc(userId, pageable);
        } else {
            // 다음 페이지 - 사용자별 커서 이후 데이터 조회
            postPage = postRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable);
        }

        List<PostListResponse> posts = postPage.getContent()
                .stream()
                .map(PostListResponse::new)
                .collect(Collectors.toList());

        boolean hasNext = postPage.hasNext();
        Long nextCursor = null;

        if (hasNext && !posts.isEmpty()) {
            nextCursor = posts.getLast().getId();
        }

        return new PostListPageResponse(posts, hasNext, nextCursor);
    }

    /**
     * 게시글 좋아요 증가 (Redis에만 저장)
     * @param postId 게시글 ID
     */
    public void incrementLikeCount(Long postId) {
        String key = LIKE_COUNT_KEY_PREFIX + postId;
        // Redis에서 좋아요 수 증가
        postRedisTemplate.opsForValue().increment(key);
        log.debug("Redis 좋아요 증가 완료 - postId: {}", postId);
    }

    /**
     * 게시글 좋아요 감소 (Redis에만 저장)
     * @param postId 게시글 ID
     */
    public void decrementLikeCount(Long postId) {
        String key = LIKE_COUNT_KEY_PREFIX + postId;
        // Redis에서 좋아요 수 감소
        postRedisTemplate.opsForValue().decrement(key);
        log.debug("Redis 좋아요 감소 완료 - postId: {}", postId);
    }

}
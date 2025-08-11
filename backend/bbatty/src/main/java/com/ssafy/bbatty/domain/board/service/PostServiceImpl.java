package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.request.PostUpdateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostDetailResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostListPageResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostListResponse;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.entity.PostImage;
import com.ssafy.bbatty.domain.board.kafka.PostEventKafkaProducer;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostImageService postImageService;
    private final PostCountService postCountService;
    private final PostEventKafkaProducer postEventKafkaProducer;
    private static final int PAGE_SIZE = 5; // 한 번에 가져올 게시글 수

    /*
    게시물 생성 메소드
     */
    @Override
    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, Long userId) {
        User user = (User) userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        
        // 사용자가 자신의 팀에만 게시글을 작성할 수 있도록 검증
        if (!user.getTeam().getId().equals(request.getTeamId())) {

            throw new ApiException(ErrorCode.FORBIDDEN_TEAM);
        }
        
        Post post = new Post(
                user,
                request.getTeamId(),
                request.getTitle(),
                request.getContent(),
                request.getIsSameTeam()
        );
        Post savedPost = postRepository.save(post);
        postImageService.processImagesInContent(request.getContent(), savedPost);
        return new PostCreateResponse(savedPost.getId(), "게시글이 성공적으로 작성되었습니다.");
    }

    /*
    게시물 삭제 메서드
     */
    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        // 작성물 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // 이미 삭제된 게시글인지 확인
        if (post.getIsDeleted()) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        // 연관된 이미지들을 소프트 삭제 처리
        postImageService.softDeleteImagesForPost(postId);

        post.setIsDeleted(true);
        postRepository.save(post);
    }

    /*
    게시물 수정 메서드
     */
    @Override
    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request, Long userId) {
        // 게시글 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // 이미 삭제된 게시글인지 확인
        if (post.getIsDeleted()) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        // 작성자 본인인지 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        // 게시글 내용 업데이트
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setIsSameTeam(request.getIsSameTeam());

        // 새로운 내용의 이미지들을 처리
        postImageService.processImagesInContent(request.getContent(), post);

        postRepository.save(post);
    }

    /*
    전체 게시물 최신 목록 조회
    */
    public PostListPageResponse getPostList(Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Post> postPage;

        if (cursor == null) {
            // 첫 페이지 - 전체 조회 (삭제되지 않은 게시글만)
            postPage = postRepository.findByIsDeletedFalseOrderByIdDesc(pageable);
        } else {
            // 다음 페이지 - 커서 이후 데이터 조회 (삭제되지 않은 게시글만)
            postPage = postRepository.findByIsDeletedFalseAndIdLessThanOrderByIdDesc(cursor, pageable);
        }

        List<PostListResponse> posts = postPage.getContent()
                .stream()
                .map(post -> {
                    PostListResponse response = new PostListResponse(post);
                    response.setViewCount(postCountService.getViewCount(post.getId()));
                    response.setLikeCount(postCountService.getLikeCount(post.getId()));
                    response.setCommentCount(postCountService.getCommentCount(post.getId()));
                    return response;
                })
                .collect(Collectors.toList());

        boolean hasNext = postPage.hasNext();
        Long nextCursor = null;

        if (hasNext && !posts.isEmpty()) {
            nextCursor = posts.getLast().getId();
        }

        return new PostListPageResponse(posts, hasNext, nextCursor);
    }

    /*
    팀별 게시물 최신 목록 조회
    */
    @Override
    public PostListPageResponse getPostListByTeam(Long teamId, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Post> postPage;

        if (cursor == null) {
            // 첫 페이지 - 팀별 조회 (삭제되지 않은 게시글만)
            postPage = postRepository.findByIsDeletedFalseAndTeamIdOrderByIdDesc(teamId, pageable);
        } else {
            // 다음 페이지 - 팀별 커서 이후 데이터 조회 (삭제되지 않은 게시글만)
            postPage = postRepository.findByIsDeletedFalseAndTeamIdAndIdLessThanOrderByIdDesc(teamId, cursor, pageable);
        }

        List<PostListResponse> posts = postPage.getContent()
                .stream()
                .map(post -> {
                    PostListResponse response = new PostListResponse(post);
                    response.setViewCount(postCountService.getViewCount(post.getId()));
                    response.setLikeCount(postCountService.getLikeCount(post.getId()));
                    response.setCommentCount(postCountService.getCommentCount(post.getId()));
                    return response;
                })
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
    public PostDetailResponse getPostDetail(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // 삭제된 게시글인지 확인
        if (post.getIsDeleted()) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        postCountService.incrementViewCount(postId, userId);
        
        // 3일 이내 작성된 글인지 확인 후 Kafka 이벤트 전송
        if (post.getCreatedAt().isAfter(LocalDateTime.now().minusDays(3))) {
            postEventKafkaProducer.sendViewEvent(postId, userId, post.getTeamId());
        }

        return PostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .authorNickname(post.getUser().getNickname())
                .content(post.getContent())
                .viewCount(postCountService.getViewCount(postId))
                .likeCount(postCountService.getLikeCount(postId))
                .commentCount(postCountService.getCommentCount(post.getId()))
                .createdAt(post.getCreatedAt().toString())
                .updatedAt(post.getUpdatedAt().toString())
                .build();
    }

    /*
    사용자 별 게시물 전체 조회
    */
    @Override
    public PostListPageResponse getPostListByUser(Long userId, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Post> postPage;

        // 해당 유저가 존재하는지는 유저 구현되면 작성


        if (cursor == null) {
            // 첫 페이지 - 사용자별 조회 (삭제되지 않은 게시글만)
            postPage = postRepository.findByIsDeletedFalseAndUserIdOrderByIdDesc(userId, pageable);
        } else {
            // 다음 페이지 - 사용자별 커서 이후 데이터 조회 (삭제되지 않은 게시글만)
            postPage = postRepository.findByIsDeletedFalseAndUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable);
        }

        List<PostListResponse> posts = postPage.getContent()
                .stream()
                .map(post -> {
                    PostListResponse response = new PostListResponse(post);
                    response.setViewCount(postCountService.getViewCount(post.getId()));
                    response.setLikeCount(postCountService.getLikeCount(post.getId()));
                    response.setCommentCount(postCountService.getCommentCount(post.getId()));
                    return response;
                })
                .collect(Collectors.toList());

        boolean hasNext = postPage.hasNext();
        Long nextCursor = null;

        if (hasNext && !posts.isEmpty()) {
            nextCursor = posts.getLast().getId();
        }

        return new PostListPageResponse(posts, hasNext, nextCursor);
    }

    /*
    팀별 게시글 제목 검색
    */
    @Override
    public PostListPageResponse searchPostsByTeam(Long teamId, String keyword, Long cursor) {
        // 검색 키워드 전처리
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }
        
        // MySQL FULLTEXT 검색을 위한 키워드 포맷팅
        String formattedKeyword = keyword.trim();
        
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Post> postPage;
        log.info("[검색 디버깅] teamId : {}, keyword : {}, formattedKeyword : {}, pageable : {}", teamId, keyword, formattedKeyword, pageable);

        if (cursor == null) {
            // 첫 페이지 - 팀별 제목 검색
            postPage = postRepository.findByTeamIdAndTitleSearchOrderByIdDesc(teamId, formattedKeyword, pageable);
        } else {
            // 다음 페이지 - 팀별 제목 검색 + 커서 기반 페이징
            postPage = postRepository.findByTeamIdAndTitleSearchAndIdLessThanOrderByIdDesc(teamId, formattedKeyword, cursor, pageable);
        }

        List<PostListResponse> posts = postPage.getContent()
                .stream()
                .map(post -> {
                    PostListResponse response = new PostListResponse(post);
                    response.setViewCount(postCountService.getViewCount(post.getId()));
                    response.setLikeCount(postCountService.getLikeCount(post.getId()));
                    response.setCommentCount(postCountService.getCommentCount(post.getId()));
                    return response;
                })
                .collect(Collectors.toList());

        boolean hasNext = postPage.hasNext();
        Long nextCursor = null;

        if (hasNext && !posts.isEmpty()) {
            nextCursor = posts.getLast().getId();
        }

        return new PostListPageResponse(posts, hasNext, nextCursor);
    }

}
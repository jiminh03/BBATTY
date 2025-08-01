package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostDetailResponse;
import com.ssafy.bbatty.domain.board.dto.response.PostListPageResponse;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.repository.PostRepository;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {
    /*

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostImageService postImageService;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    @DisplayName("게시글 생성 성공")
    void createPost_Success() {
        // Given
        Long userId = 1L;
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(userId, "testuser", team);
        PostCreateRequest request = new PostCreateRequest("제목", "내용", 1L, false);
        
        Post savedPost = new Post(user, 1L, "제목", "내용", false);
        savedPost.setId(1L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        doNothing().when(postImageService).processImagesInContent(anyString(), any(Post.class));

        // When
        PostCreateResponse response = postService.createPost(request, userId);

        // Then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getMessage()).isEqualTo("게시글이 성공적으로 작성되었습니다.");
        
        verify(userRepository).findById(userId);
        verify(postRepository).save(any(Post.class));
        verify(postImageService).processImagesInContent(request.getContent(), savedPost);
    }

    @Test
    @DisplayName("게시글 생성 실패 - 사용자 없음")
    void createPost_UserNotFound() {
        // Given
        Long userId = 999L;
        PostCreateRequest request = new PostCreateRequest("제목", "내용", 1L, false);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> postService.createPost(request, userId))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
        
        verify(userRepository).findById(userId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(userId, "testuser", team);
        Post post = createPost(postId, user, "제목", "내용");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        doNothing().when(postImageService).deleteImagesForPost(postId);
        doNothing().when(postRepository).delete(post);

        // When
        postService.deletePost(postId, userId);

        // Then
        verify(postRepository).findById(postId);
        verify(postImageService).deleteImagesForPost(postId);
        verify(postRepository).delete(post);
    }



    @Test
    @DisplayName("게시글 삭제 실패 - 권한 없음")
    void deletePost_Forbidden() {
        // Given
        Long postId = 1L;
        Long userId = 1L;
        Long differentUserId = 2L;
        
        Team team = createTeam(1L, "테스트팀");
        User postOwner = createUser(differentUserId, "postowner", team);
        Post post = createPost(postId, postOwner, "제목", "내용");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When & Then
        assertThatThrownBy(() -> postService.deletePost(postId, userId))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_FORBIDDEN);
        
        verify(postRepository).findById(postId);
        verify(postImageService, never()).deleteImagesForPost(anyLong());
        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("게시글 목록 조회 성공 - 첫 페이지")
    void getPostList_FirstPage() {
        // Given
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(1L, "testuser", team);
        List<Post> posts = Arrays.asList(
                createPost(3L, user, "제목3", "내용3"),
                createPost(2L, user, "제목2", "내용2"),
                createPost(1L, user, "제목1", "내용1")
        );
        
        Pageable pageable = PageRequest.of(0, 5);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 10); // 총 10개 있다고 가정

        when(postRepository.findAllByOrderByIdDesc(pageable)).thenReturn(postPage);

        // When
        PostListPageResponse response = postService.getPostList(null);

        // Then
        assertThat(response.getPosts()).hasSize(3);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo(1L);
        
        verify(postRepository).findAllByOrderByIdDesc(pageable);
        verify(postRepository, never()).findByIdLessThanOrderByIdDesc(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("게시글 목록 조회 성공 - 다음 페이지")
    void getPostList_NextPage() {
        // Given
        Long cursor = 5L;
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(1L, "testuser", team);
        List<Post> posts = Arrays.asList(
                createPost(4L, user, "제목4", "내용4"),
                createPost(3L, user, "제목3", "내용3")
        );
        
        Pageable pageable = PageRequest.of(0, 5);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 2); // 더 이상 없음

        when(postRepository.findByIdLessThanOrderByIdDesc(cursor, pageable)).thenReturn(postPage);

        // When
        PostListPageResponse response = postService.getPostList(cursor);

        // Then
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();
        
        verify(postRepository).findByIdLessThanOrderByIdDesc(cursor, pageable);
        verify(postRepository, never()).findAllByOrderByIdDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("팀별 게시글 목록 조회 성공 - 첫 페이지")
    void getPostListByTeam_FirstPage() {
        // Given
        Long teamId = 1L;
        Team team = createTeam(teamId, "테스트팀");
        User user = createUser(1L, "testuser", team);
        List<Post> posts = Arrays.asList(
                createPost(3L, user, "제목3", "내용3"),
                createPost(2L, user, "제목2", "내용2"),
                createPost(1L, user, "제목1", "내용1")
        );
        
        Pageable pageable = PageRequest.of(0, 5);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 10); // 총 10개 있다고 가정

        when(postRepository.findByTeamIdOrderByIdDesc(teamId, pageable)).thenReturn(postPage);

        // When
        PostListPageResponse response = postService.getPostListByTeam(teamId, null);

        // Then
        assertThat(response.getPosts()).hasSize(3);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo(1L);
        
        verify(postRepository).findByTeamIdOrderByIdDesc(teamId, pageable);
        verify(postRepository, never()).findByTeamIdAndIdLessThanOrderByIdDesc(anyLong(), anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("팀별 게시글 목록 조회 성공 - 다음 페이지")
    void getPostListByTeam_NextPage() {
        // Given
        Long teamId = 1L;
        Long cursor = 5L;
        Team team = createTeam(teamId, "테스트팀");
        User user = createUser(1L, "testuser", team);
        List<Post> posts = Arrays.asList(
                createPost(4L, user, "제목4", "내용4"),
                createPost(3L, user, "제목3", "내용3")
        );
        
        Pageable pageable = PageRequest.of(0, 5);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 2); // 더 이상 없음

        when(postRepository.findByTeamIdAndIdLessThanOrderByIdDesc(teamId, cursor, pageable)).thenReturn(postPage);

        // When
        PostListPageResponse response = postService.getPostListByTeam(teamId, cursor);

        // Then
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();
        
        verify(postRepository).findByTeamIdAndIdLessThanOrderByIdDesc(teamId, cursor, pageable);
        verify(postRepository, never()).findByTeamIdOrderByIdDesc(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("팀별 게시글 목록 조회 - 빈 결과")
    void getPostListByTeam_EmptyResult() {
        // Given
        Long teamId = 999L; // 존재하지 않는 팀
        Pageable pageable = PageRequest.of(0, 5);
        Page<Post> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(postRepository.findByTeamIdOrderByIdDesc(teamId, pageable)).thenReturn(emptyPage);

        // When
        PostListPageResponse response = postService.getPostListByTeam(teamId, null);

        // Then
        assertThat(response.getPosts()).isEmpty();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();
        
        verify(postRepository).findByTeamIdOrderByIdDesc(teamId, pageable);
    }

    @Test
    @DisplayName("게시글 상세 조회 성공")
    void getPostDetail_Success() {
        // Given
        Long postId = 1L;
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(1L, "testuser", team);
        Post post = createPost(postId, user, "테스트 제목", "테스트 내용");
        post.setUpdatedAt(LocalDateTime.now());

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When
        PostDetailResponse response = postService.getPostDetail(postId, user.getId());

        // Then
        assertThat(response.getPostId()).isEqualTo(postId);
        assertThat(response.getTitle()).isEqualTo("테스트 제목");
        assertThat(response.getAuthorNickname()).isEqualTo("testuser");
        assertThat(response.getContent()).isEqualTo("테스트 내용");
        assertThat(response.getLikeCount()).isEqualTo(0L);
        assertThat(response.getViewCount()).isEqualTo(0L);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();

        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("사용자별 게시글 목록 조회 성공 - 첫 페이지")
    void getPostListByUser_FirstPage() {
        // Given
        Long userId = 1L;
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(userId, "testuser", team);
        List<Post> posts = Arrays.asList(
                createPost(3L, user, "제목3", "내용3"),
                createPost(2L, user, "제목2", "내용2"),
                createPost(1L, user, "제목1", "내용1")
        );

        Pageable pageable = PageRequest.of(0, 5);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 10);

        when(postRepository.findByUserIdOrderByIdDesc(userId, pageable)).thenReturn(postPage);

        // When
        PostListPageResponse response = postService.getPostListByUser(userId, null);

        // Then
        assertThat(response.getPosts()).hasSize(3);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo(1L);

        verify(postRepository).findByUserIdOrderByIdDesc(userId, pageable);
        verify(postRepository, never()).findByUserIdAndIdLessThanOrderByIdDesc(anyLong(), anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("사용자별 게시글 목록 조회 성공 - 다음 페이지")
    void getPostListByUser_NextPage() {
        // Given
        Long userId = 1L;
        Long cursor = 5L;
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(userId, "testuser", team);
        List<Post> posts = Arrays.asList(
                createPost(4L, user, "제목4", "내용4"),
                createPost(3L, user, "제목3", "내용3")
        );

        Pageable pageable = PageRequest.of(0, 5);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 2);

        when(postRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable)).thenReturn(postPage);

        // When
        PostListPageResponse response = postService.getPostListByUser(userId, cursor);

        // Then
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();

        verify(postRepository).findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable);
        verify(postRepository, never()).findByUserIdOrderByIdDesc(anyLong(), any(Pageable.class));
    }


    private User createUser(Long id, String nickname, Team team) {
        return User.builder()
                .id(id)
                .nickname(nickname)
                .gender(User.Gender.MALE)
                .age(25)
                .team(team)
                .introduction("테스트 소개")
                .build();
    }

    private Team createTeam(Long id, String name) {
        return Team.builder()
                .id(id)
                .name(name)
                .wins(0)
                .draws(0)
                .loses(0)
                .build();
    }

    private Post createPost(Long id, User user, String title, String content) {
        Post post = new Post(user, 1L, title, content, false);
        post.setId(id);
        post.setCreatedAt(LocalDateTime.now());
        post.setViewCount(0L);
        return post;
    }
     */
}
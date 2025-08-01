package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.CommentCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.CommentListResponse;
import com.ssafy.bbatty.domain.board.dto.response.CommentResponse;
import com.ssafy.bbatty.domain.board.entity.Comment;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.repository.CommentRepository;
import com.ssafy.bbatty.domain.board.repository.PostRepository;
import com.ssafy.bbatty.domain.team.entity.Team;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.Gender;
import com.ssafy.bbatty.global.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    @DisplayName("댓글 생성 성공")
    void createComment_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;
        String content = "테스트 댓글 내용";
        
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(userId, "testuser", team);
        Post post = createPost(postId, user, "게시글 제목", "게시글 내용");
        
        CommentCreateRequest request = new CommentCreateRequest();
        request.setPostId(postId);
        request.setUserId(userId);
        request.setContent(content);
        request.setParentId(null); // 부모 댓글
        
        Comment savedComment = createComment(1L, user, post, content, 0, null);
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        // When
        Comment result = commentService.createComment(request);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getDepth()).isEqualTo(0);
        assertThat(result.getParent()).isNull();
        
        verify(postRepository).findById(postId);
        verify(userRepository).findById(userId);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("대댓글 생성 성공")
    void createReply_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;
        Long parentId = 1L;
        String content = "테스트 대댓글 내용";
        
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(userId, "testuser", team);
        Post post = createPost(postId, user, "게시글 제목", "게시글 내용");
        Comment parentComment = createComment(parentId, user, post, "부모 댓글", 0, null);
        
        CommentCreateRequest request = new CommentCreateRequest();
        request.setPostId(postId);
        request.setUserId(userId);
        request.setContent(content);
        request.setParentId(parentId);
        
        Comment savedReply = createComment(2L, user, post, content, 1, parentComment);
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedReply);

        // When
        Comment result = commentService.createComment(request);

        // Then
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getDepth()).isEqualTo(1);
        assertThat(result.getParent()).isEqualTo(parentComment);
        
        verify(postRepository).findById(postId);
        verify(userRepository).findById(userId);
        verify(commentRepository).findById(parentId);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 게시글 없음")
    void createComment_PostNotFound() {
        // Given
        CommentCreateRequest request = new CommentCreateRequest();
        request.setPostId(999L);
        request.setUserId(1L);
        request.setContent("테스트 댓글");

        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);
        
        verify(postRepository).findById(999L);
        verify(userRepository, never()).findById(anyLong());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 사용자 없음")
    void createComment_UserNotFound() {
        // Given
        Long postId = 1L;
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(1L, "testuser", team);
        Post post = createPost(postId, user, "게시글 제목", "게시글 내용");
        
        CommentCreateRequest request = new CommentCreateRequest();
        request.setPostId(postId);
        request.setUserId(999L);
        request.setContent("테스트 댓글");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
        
        verify(postRepository).findById(postId);
        verify(userRepository).findById(999L);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 부모 댓글 없음")
    void createComment_ParentCommentNotFound() {
        // Given
        Long postId = 1L;
        Long userId = 1L;
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(userId, "testuser", team);
        Post post = createPost(postId, user, "게시글 제목", "게시글 내용");
        
        CommentCreateRequest request = new CommentCreateRequest();
        request.setPostId(postId);
        request.setUserId(userId);
        request.setContent("테스트 대댓글");
        request.setParentId(999L);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
        
        verify(postRepository).findById(postId);
        verify(userRepository).findById(userId);
        verify(commentRepository).findById(999L);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() {
        // Given
        Long commentId = 1L;
        String newContent = "수정된 댓글 내용";
        
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(1L, "testuser", team);
        Post post = createPost(1L, user, "게시글 제목", "게시글 내용");
        Comment comment = createComment(commentId, user, post, "원본 댓글", 0, null);
        Comment updatedComment = createComment(commentId, user, post, newContent, 0, null);
        
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(updatedComment);

        // When
        Comment result = commentService.updateComment(commentId, newContent);

        // Then
        assertThat(result.getContent()).isEqualTo(newContent);
        
        verify(commentRepository).findById(commentId);
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 없음")
    void updateComment_CommentNotFound() {
        // Given
        Long commentId = 999L;
        String newContent = "수정된 댓글 내용";

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(commentId, newContent))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
        
        verify(commentRepository).findById(commentId);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_Success() {
        // Given
        Long commentId = 1L;
        Team team = createTeam(1L, "테스트팀");
        User user = createUser(1L, "testuser", team);
        Post post = createPost(1L, user, "게시글 제목", "게시글 내용");
        Comment comment = createComment(commentId, user, post, "삭제할 댓글", 0, null);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        doNothing().when(commentRepository).delete(comment);

        // When
        commentService.deleteComment(commentId);

        // Then
        verify(commentRepository).findById(commentId);
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글 없음")
    void deleteComment_CommentNotFound() {
        // Given
        Long commentId = 999L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(commentId))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
        
        verify(commentRepository).findById(commentId);
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 댓글과 대댓글 포함")
    void getCommentsWithReplies_Success() {
        // Given
        Long postId = 1L;
        Team team = createTeam(1L, "테스트팀");
        User user1 = createUser(1L, "user1", team);
        User user2 = createUser(2L, "user2", team);
        Post post = createPost(postId, user1, "게시글 제목", "게시글 내용");
        
        // 부모 댓글들
        Comment parentComment1 = createComment(1L, user1, post, "첫 번째 댓글", 0, null);
        Comment parentComment2 = createComment(2L, user2, post, "두 번째 댓글", 0, null);
        List<Comment> parentComments = Arrays.asList(parentComment1, parentComment2);
        
        // 첫 번째 댓글의 대댓글들
        Comment reply1 = createComment(3L, user2, post, "첫 번째 대댓글", 1, parentComment1);
        Comment reply2 = createComment(4L, user1, post, "두 번째 대댓글", 1, parentComment1);
        List<Comment> repliesForComment1 = Arrays.asList(reply1, reply2);
        
        // 두 번째 댓글은 대댓글 없음
        List<Comment> repliesForComment2 = Arrays.asList();

        when(commentRepository.findByPostIdAndDepthOrderByCreatedAtAsc(postId, 0)).thenReturn(parentComments);
        when(commentRepository.findByParentIdOrderByCreatedAtAsc(1L)).thenReturn(repliesForComment1);
        when(commentRepository.findByParentIdOrderByCreatedAtAsc(2L)).thenReturn(repliesForComment2);

        // When
        CommentListResponse result = commentService.getCommentsWithRepliesByPostId(postId);

        // Then
        assertThat(result.getComments()).hasSize(2);
        
        // 첫 번째 댓글 검증
        CommentResponse firstComment = result.getComments().get(0);
        assertThat(firstComment.getId()).isEqualTo(1L);
        assertThat(firstComment.getContent()).isEqualTo("첫 번째 댓글");
        assertThat(firstComment.getNickname()).isEqualTo("user1");
        assertThat(firstComment.getDepth()).isEqualTo(0);
        assertThat(firstComment.getReplies()).hasSize(2);
        
        // 첫 번째 댓글의 대댓글들 검증
        CommentResponse firstReply = firstComment.getReplies().get(0);
        assertThat(firstReply.getId()).isEqualTo(3L);
        assertThat(firstReply.getContent()).isEqualTo("첫 번째 대댓글");
        assertThat(firstReply.getNickname()).isEqualTo("user2");
        assertThat(firstReply.getDepth()).isEqualTo(1);
        
        // 두 번째 댓글 검증
        CommentResponse secondComment = result.getComments().get(1);
        assertThat(secondComment.getId()).isEqualTo(2L);
        assertThat(secondComment.getContent()).isEqualTo("두 번째 댓글");
        assertThat(secondComment.getReplies()).isEmpty();
        
        verify(commentRepository).findByPostIdAndDepthOrderByCreatedAtAsc(postId, 0);
        verify(commentRepository).findByParentIdOrderByCreatedAtAsc(1L);
        verify(commentRepository).findByParentIdOrderByCreatedAtAsc(2L);
    }

    @Test
    @DisplayName("댓글 목록 조회 - 빈 결과")
    void getCommentsWithReplies_EmptyResult() {
        // Given
        Long postId = 1L;
        when(commentRepository.findByPostIdAndDepthOrderByCreatedAtAsc(postId, 0)).thenReturn(Arrays.asList());

        // When
        CommentListResponse result = commentService.getCommentsWithRepliesByPostId(postId);

        // Then
        assertThat(result.getComments()).isEmpty();
        
        verify(commentRepository).findByPostIdAndDepthOrderByCreatedAtAsc(postId, 0);
    }

    private User createUser(Long id, String nickname, Team team) {
        return User.builder()
                .id(id)
                .nickname(nickname)
                .gender(Gender.MALE)
                .age(25)
                .team(team)
                .introduction("테스트 소개")
                .profileImg("https://example.com/profile.jpg")
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
        post.setLikeCount(0L);
        return post;
    }

    private Comment createComment(Long id, User user, Post post, String content, int depth, Comment parent) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setUser(user);
        comment.setPost(post);
        comment.setContent(content);
        comment.setDepth(depth);
        comment.setParent(parent);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        return comment;
    }
}
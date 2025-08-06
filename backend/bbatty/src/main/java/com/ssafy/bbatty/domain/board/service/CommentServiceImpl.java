package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.CommentCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.CommentListPageResponse;
import com.ssafy.bbatty.domain.board.dto.response.CommentListResponse;
import com.ssafy.bbatty.domain.board.dto.response.CommentResponse;
import com.ssafy.bbatty.domain.board.entity.Comment;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.kafka.PostEventKafkaProducer;
import com.ssafy.bbatty.domain.board.repository.CommentRepository;
import com.ssafy.bbatty.domain.board.repository.PostRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService{

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostEventKafkaProducer postEventKafkaProducer;
    private static final int PAGE_SIZE = 10; // 한 번에 가져올 댓글 수

    @Override
    public Comment createComment(CommentCreateRequest request) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        Comment comment;
        
        if (request.getParentId() != null) {
            // 대댓글 생성
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
            comment = new Comment(post, user, request.getContent(), parent);
        } else {
            // 일반 댓글 생성
            comment = new Comment(post, user, request.getContent());
        }

        Comment savedComment = commentRepository.save(comment);
        
        // 댓글 이벤트를 Kafka로 전송
        postEventKafkaProducer.sendCommentEvent(post.getId(), request.getUserId(), post.getTeamId(), savedComment.getId());
        
        return savedComment;
    }

    @Override
    public List<Comment> getCommentsByPostId(Long postId) {
        return List.of();
    }

    @Override
    public Comment updateComment(Long id, String content) {

         Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));

        comment.setContent(content);

        return commentRepository.save(comment);

    }

    @Override
    public CommentListResponse getCommentsWithRepliesByPostId(Long postId) {
        // 1. 부모 댓글들 조회 (depth = 0)
        List<Comment> parentComments = commentRepository.findByPostIdAndDepthOrderByCreatedAtAsc(postId, 0);
        
        // 2. 각 부모 댓글에 대한 대댓글들 조회하여 CommentResponse 생성
        List<CommentResponse> commentResponses = parentComments.stream()
                .map(parentComment -> {
                    // 해당 부모 댓글의 대댓글들 조회
                    List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parentComment.getId());
                    
                    // 대댓글들을 CommentResponse로 변환
                    List<CommentResponse> replyResponses = replies.stream()
                            .map(CommentResponse::new)
                            .collect(Collectors.toList());
                    
                    // 부모 댓글을 CommentResponse로 변환 (대댓글 포함)
                    return new CommentResponse(parentComment, replyResponses);
                })
                .collect(Collectors.toList());
        
        return new CommentListResponse(commentResponses);
    }

    @Override
    public CommentListPageResponse getCommentsWithRepliesByPostIdWithPagination(Long postId, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Comment> commentPage;

        if (cursor == null) {
            // 첫 페이지 - 부모 댓글들만 조회 (depth = 0)
            commentPage = commentRepository.findByPostIdAndDepthOrderByIdDesc(postId, 0, pageable);
        } else {
            // 다음 페이지 - 커서 이후 데이터 조회
            commentPage = commentRepository.findByPostIdAndDepthAndIdLessThanOrderByIdDesc(postId, 0, cursor, pageable);
        }

        List<CommentResponse> commentResponses = commentPage.getContent()
                .stream()
                .map(parentComment -> {
                    // 해당 부모 댓글의 대댓글들 조회
                    List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parentComment.getId());
                    
                    // 대댓글들을 CommentResponse로 변환
                    List<CommentResponse> replyResponses = replies.stream()
                            .map(CommentResponse::new)
                            .collect(Collectors.toList());
                    
                    // 부모 댓글을 CommentResponse로 변환 (대댓글 포함)
                    return new CommentResponse(parentComment, replyResponses);
                })
                .collect(Collectors.toList());

        boolean hasNext = commentPage.hasNext();
        Long nextCursor = null;

        if (hasNext && !commentResponses.isEmpty()) {
            nextCursor = commentResponses.getLast().getId();
        }

        return new CommentListPageResponse(commentResponses, hasNext, nextCursor);
    }

    @Override
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));

        // 소프트 삭제 처리
        comment.setIsDeleted(true);
        commentRepository.save(comment);
    }

}

package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.CommentCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.CommentListResponse;
import com.ssafy.bbatty.domain.board.dto.response.CommentResponse;
import com.ssafy.bbatty.domain.board.entity.Comment;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.repository.CommentRepository;
import com.ssafy.bbatty.domain.board.repository.PostRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
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

    @Override
    public Comment createComment(CommentCreateRequest request) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(request.getContent());
        comment.setDepth(request.getParentId() == null ? 0 : 1);


        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
            comment.setParent(parent);
        }

        return commentRepository.save(comment);
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
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));

        commentRepository.delete(comment);
    }

}

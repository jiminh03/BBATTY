package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.repository.PostRepository;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostImageService postImageService;

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

        postImageService.processImagesInContent(request.getContent(), savedPost);
        
        return new PostCreateResponse(savedPost.getId(), "게시글이 성공적으로 작성되었습니다.");
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // 작성자 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        // 먼저 S3에서 이미지 삭제
        postImageService.deleteImagesForPost(postId);
        
        // 그 다음 게시글 삭제 (PostImage는 CASCADE로 자동 삭제됨)
        postRepository.delete(post);
    }

}
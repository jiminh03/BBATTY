package com.ssafy.bbatty.domain.board.service;

import com.ssafy.bbatty.domain.board.dto.request.PostCreateRequest;
import com.ssafy.bbatty.domain.board.dto.response.PostCreateResponse;
import com.ssafy.bbatty.domain.board.entity.Post;
import com.ssafy.bbatty.domain.board.repository.PostRepository;
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
    //private final UserRepository userRepository;
    private final PostImageService postImageService;
    /**/
    @Override
    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, Long userId) {
    /*    User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        
        Post post = new Post(
                user,
                request.getTeamId(),
                request.getTitle(),
                request.getContent(),
                request.getIsSameTeam()
        );
        
        Post savedPost = postRepository.save(post);
        
        // 컨텐츠에 포함된 이미지들을 USED로 변경하고, 사용되지 않은 이미지들은 삭제
        postImageService.processImagesInContent(request.getContent(), savedPost);
        
        return new PostCreateResponse(savedPost.getId(), "게시글이 성공적으로 작성되었습니다.");
    */
    return null;
    }


}
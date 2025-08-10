import { useMutation, useInfiniteQuery, useQueryClient, useQuery } from '@tanstack/react-query'
import { postApi } from '../api/api'
import { CreatePostPayload, CursorPostListResponse } from '../api/types'
import { Post } from '../model/types';

// 무한스크롤 게시글 목록
export const usePostListQuery = (teamId: number) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts', teamId],                               // ✅ teamId 포함
    queryFn: ({ pageParam = undefined }) =>
      postApi.getPosts(teamId, pageParam as number | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => (last?.hasNext ? last.nextCursor : undefined),
  });

// 게시글 생성
export const useCreatePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatePostPayload) => postApi.createPost(payload),
    onSuccess: (_, vars) => {
      // teamId별 캐시 무효화
      qc.invalidateQueries({ queryKey: ['posts', vars.teamId] });
    },
  });
};

// 게시글 상세보기
export const usePostDetailQuery = (postId: number) => {
  return useQuery<Post>({
    queryKey: ['post', postId],
    queryFn: () => postApi.getPostById(postId),
  });
};
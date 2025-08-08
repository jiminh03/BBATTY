import { useMutation, useInfiniteQuery, useQueryClient } from '@tanstack/react-query'
import { postApi } from '../api/api'
import { CreatePostPayload, CursorPostListResponse } from '../api/types'

// 무한스크롤 게시글 목록
export const usePostListQuery = () => {
  return useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts'],
    queryFn: ({ pageParam = undefined }) =>
      postApi.getPosts(1, pageParam as number), // teamId 하드코딩 예시
    initialPageParam: undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.nextCursor : undefined,
  });
};

// 게시글 생성
export const useCreatePost = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatePostPayload) => postApi.createPost(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['posts'] }); // 목록 새로고침
    },
  });
};

import { useMutation, useInfiniteQuery, useQueryClient } from '@tanstack/react-query'
import { postApi } from '../api/api'
import { CreatePostPayload, CursorPostListResponse } from '../api/types'

// 무한스크롤 게시글 목록
export const usePostListQuery = (teamId: number) => {           // ✅ teamId 받기
  return useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts', teamId],
    queryFn: ({ pageParam = undefined }) => postApi.getPosts(teamId, pageParam as number),
    initialPageParam: undefined,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.nextCursor : undefined),
  });
};

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
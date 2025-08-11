// entities/post/queries/usePostQueries.ts
import { useMutation, useInfiniteQuery, useQueryClient, useQuery } from '@tanstack/react-query';
import { postApi } from '../api/api';
import { CreatePostPayload, CursorPostListResponse } from '../api/types';
import { Post } from '../model/types';

export const usePostListQuery = (teamId: number) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts', teamId],
    queryFn: ({ pageParam = undefined }) => postApi.getPosts(teamId, pageParam as number | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => (last?.hasNext ? last.nextCursor : undefined),
  });

export const useCreatePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatePostPayload) => postApi.createPost(payload),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['posts', vars.teamId] });
    },
  });
};

export const usePostDetailQuery = (postId: number) =>
  useQuery<Post>({ queryKey: ['post', postId], queryFn: () => postApi.getPostById(postId) });

// ✅ 새로 추가: 게시글 삭제
export const useDeletePostMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (postId: number) => postApi.deletePost(String(postId)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['posts'] }); // 리스트 갱신
    },
  });
};

export const useUpdatePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { postId: number; title: string; content: string; teamId?: number; status?: string; postStatus?: string }) =>
      postApi.updatePost(vars.postId, vars),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ['post', vars.postId] });
      if (vars.teamId) qc.invalidateQueries({ queryKey: ['posts', vars.teamId] });
    },
  });
};
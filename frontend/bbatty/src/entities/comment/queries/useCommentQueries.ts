// entities/comment/queries/useCommentQueries.ts
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { commentApi } from '../api/api';
import { CreateCommentPayload, DeleteCommentPayload, UpdateCommentPayload, CommentListResponse } from '../api/types';
import { useAuthStore } from '../../auth/model/authStore';
import type { InfiniteData } from '@tanstack/react-query';

export const useCommentListQuery = (postId: number, pageSize = 10) =>
  useInfiniteQuery<
    CommentListResponse,
    Error,
    InfiniteData<CommentListResponse, number>, // ✅ data는 InfiniteData가 됨
    ['comments', number, number],
    number                    
  >({
    queryKey: ['comments', postId, pageSize],
    queryFn: ({ pageParam = 0 }) =>
      commentApi.getComments({ postId, page: pageParam, size: pageSize }),
    initialPageParam: 0,
    getNextPageParam: (last) =>
      typeof last.hasMore === 'boolean'
        ? (last.hasMore ? last.page + 1 : undefined)
        : ((last.page + 1) * (last.limit ?? pageSize) < (last.totalCount ?? 0)
            ? last.page + 1
            : undefined),
  });

export const useCreateComment = (postId: number) => {
  const qc = useQueryClient();
  const userId = 1;
  //useAuthStore((s) => s.kakaoUserInfo?.id);

  return useMutation({
    mutationFn: (content: string) => {
      if (!userId) throw new Error('로그인이 필요합니다.');
      const payload: CreateCommentPayload = { postId, userId:1, content, parentId: null };
      return commentApi.createComment(payload);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['comments', postId] });
      qc.invalidateQueries({ queryKey: ['post', postId] });
    },
  });
};

export const useUpdateComment = (postId: number) => {
  const qc = useQueryClient();
  const userId = useAuthStore((s) => s.kakaoUserInfo?.id);

  return useMutation({
    mutationFn: (payload: Omit<UpdateCommentPayload, 'userId'>) => {
      if (!userId) throw new Error('로그인이 필요합니다.');
      return commentApi.updateComment({ ...payload /*, userId*/ });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['comments', postId] }),
  });
};

export const useDeleteComment = (postId: number) => {
  const qc = useQueryClient();
  const userId = useAuthStore((s) => s.kakaoUserInfo?.id);

  return useMutation({
    mutationFn: (payload: DeleteCommentPayload) => {
      if (!userId) throw new Error('로그인이 필요합니다.');
      return commentApi.deleteComment({ ...payload /*, userId*/ });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['comments', postId] });
      qc.invalidateQueries({ queryKey: ['post', postId] });
    },
  });
};

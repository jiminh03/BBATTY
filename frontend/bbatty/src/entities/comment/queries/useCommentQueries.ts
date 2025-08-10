// entities/comment/queries/useCommentQueries.ts
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { commentApi } from '../api/api';
import type { CommentListResponse, CreateCommentPayload, DeleteCommentPayload, UpdateCommentPayload } from '../api/types';
import { useUserStore } from '../../user/model/userStore';

export const useCommentListQuery = (postId: number, pageSize = 10) =>
  useInfiniteQuery<CommentListResponse, Error, CommentListResponse, ['comments', number, number], number>({
    queryKey: ['comments', postId, pageSize],
    queryFn: ({ pageParam = 0 }) => commentApi.getComments({ postId, page: pageParam, size: pageSize }),
    initialPageParam: 0,
    getNextPageParam: (last) =>
      typeof last.hasMore === 'boolean'
        ? (last.hasMore ? last.page + 1 : undefined)
        : (((last.page + 1) * (last.limit ?? pageSize) < (last.totalCount ?? 0)) ? last.page + 1 : undefined),
  });

export const useCreateComment = (postId: number) => {
  const qc = useQueryClient();
  const userId = useUserStore((s) => s.currentUser?.userId); // ✅ authStore 대신 userStore 사용

  return useMutation({
    mutationFn: (content: string) => {
      if (!userId) throw new Error('로그인이 필요합니다.');
      const payload: CreateCommentPayload = { postId, userId, content, parentId: null };
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
  const userId = useUserStore((s) => s.currentUser?.userId);

  return useMutation({
    mutationFn: (payload: Omit<UpdateCommentPayload, 'userId'>) => {
      if (!userId) throw new Error('로그인이 필요합니다.');
      return commentApi.updateComment({ ...payload });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['comments', postId] }),
  });
};

export const useDeleteComment = (postId: number) => {
  const qc = useQueryClient();
  const userId = useUserStore((s) => s.currentUser?.userId);

  return useMutation({
    mutationFn: (payload: DeleteCommentPayload) => {
      if (!userId) throw new Error('로그인이 필요합니다.');
      return commentApi.deleteComment(payload);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['comments', postId] });
      qc.invalidateQueries({ queryKey: ['post', postId] });
    },
  });
};

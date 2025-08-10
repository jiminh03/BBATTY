// entities/comment/queries/useCommentQueries.ts
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { commentApi } from '../api/api';
import { CreateCommentPayload, DeleteCommentPayload, UpdateCommentPayload, CommentListResponse } from '../api/types';
import { useUserStore } from '../../user/model/userStore'; // 카카오 로그인으로 저장된 유저

export const useCommentListQuery = (postId: number, pageSize = 10) =>
  useInfiniteQuery<CommentListResponse, Error, CommentListResponse, ['comments', number, number], number>({
    queryKey: ['comments', postId, pageSize],
    queryFn: ({ pageParam = 0 }) => commentApi.getComments({ postId, page: pageParam, size: pageSize }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasMore ? last.page + 1 : undefined),
  });

// entities/comment/queries/useCommentQueries.ts (일부분)
export const useCreateComment = (postId: number) => {
  const qc = useQueryClient();
  const userId = useUserStore((s) => s.currentUser?.userId);

  return useMutation({
    mutationFn: async (content: string) => {
      if (!userId) throw new Error('로그인이 필요합니다.');
      const payload: CreateCommentPayload = { postId, userId, content, parentId: null };
      await commentApi.createComment(payload);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['comments', postId] });
      qc.invalidateQueries({ queryKey: ['post', postId] });
    },
    onError: (e: any) => {
      console.log('[useCreateComment][ERROR]', e?.message, e);
    },
  });
};


export const useUpdateComment = (postId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateCommentPayload) => commentApi.updateComment(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['comments', postId] }),
  });
};

export const useDeleteComment = (postId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: DeleteCommentPayload) => commentApi.deleteComment(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['comments', postId] });
      qc.invalidateQueries({ queryKey: ['post', postId] });
    },
  });
};

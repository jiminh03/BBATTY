// entities/comment/queries/useCommentQueries.ts
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { commentApi } from '../api/api';
import { CreateCommentPayload, DeleteCommentPayload, UpdateCommentPayload, CommentListResponse } from '../api/types';
import { useUserStore } from '../../user/model/userStore';
import { syncCommentCountEverywhere } from '../../post/queries/usePostQueries';
import type { Post } from '../../post/model/types';

/** 댓글 목록 (페이지네이션) */
export const useCommentListQuery = (postId: number, pageSize = 10) =>
  useInfiniteQuery<CommentListResponse>({
    queryKey: ['comments', postId, pageSize],
    queryFn: ({ pageParam = 0 }) => commentApi.getComments({ postId, page: pageParam as number, size: pageSize }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasMore ? last.page + 1 : undefined),
  });

/** 캐시에서 teamId 추출 (없으면 undefined) */
function getTeamIdFromCache(qc: ReturnType<typeof useQueryClient>, postId: number): number | undefined {
  const p = qc.getQueryData<Post>(['post', postId]);
  const v = (p as any)?.teamId;
  return typeof v === 'number' ? v : undefined;
}

/** 댓글 생성 */
export const useCreateComment = (postId: number) => {
  const qc = useQueryClient();
  const userId = useUserStore((s) => s.currentUser?.userId ?? null);

  return useMutation({
    mutationFn: async (content: string) => {
      if (!userId) throw new Error('로그인이 필요합니다.');
      const payload: CreateCommentPayload = { postId, userId, content, parentId: null };
      await commentApi.createComment(payload);
    },
    onSuccess: () => {
      // 목록 갱신
      qc.invalidateQueries({ queryKey: ['comments', postId] });
      // 댓글 수 +1 전파
      const teamId = getTeamIdFromCache(qc, postId);
      syncCommentCountEverywhere(qc, postId, +1, { teamId, userId });
      // 상세 재검증(선택)
      qc.invalidateQueries({ queryKey: ['post', postId] });
    },
    onError: (e: any) => {},
  });
};

/** 댓글 수정 */
export const useUpdateComment = (postId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateCommentPayload) => commentApi.updateComment(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['comments', postId] });
    },
  });
};

/** 댓글 삭제 */
export const useDeleteComment = (postId: number) => {
  const qc = useQueryClient();
  const userId = useUserStore((s) => s.currentUser?.userId ?? null);

  return useMutation({
    mutationFn: (payload: DeleteCommentPayload) => commentApi.deleteComment(payload),
    onSuccess: () => {
      // 목록 갱신
      qc.invalidateQueries({ queryKey: ['comments', postId] });
      // 댓글 수 -1 전파
      const teamId = getTeamIdFromCache(qc, postId);
      syncCommentCountEverywhere(qc, postId, -1, { teamId, userId });
      // 상세 재검증(선택)
      qc.invalidateQueries({ queryKey: ['post', postId] });
    },
  });
};

/** 답글 생성 */
export const useCreateReply = (postId: number, parentId: number) => {
  const qc = useQueryClient();
  const userId = useUserStore((s) => s.currentUser?.userId ?? null);

  return useMutation({
    mutationFn: async (content: string) => {
      if (!userId) throw new Error('로그인이 필요합니다.');
      const payload: CreateCommentPayload = { postId, userId, content, parentId };
      await commentApi.createComment(payload);
    },
    onSuccess: () => {
      // 목록 갱신
      qc.invalidateQueries({ queryKey: ['comments', postId] });
      // 댓글 수 +1 전파
      const teamId = getTeamIdFromCache(qc, postId);
      syncCommentCountEverywhere(qc, postId, +1, { teamId, userId });
      // 상세 재검증(선택)
      qc.invalidateQueries({ queryKey: ['post', postId] });
    },
  });
};

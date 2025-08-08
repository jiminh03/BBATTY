// entities/comment/api/api.ts
import { apiClient } from '../../../shared/api/client/apiClient';
import {
  CreateCommentPayload,
  UpdateCommentPayload,
  DeleteCommentPayload,
  GetCommentsParams,
  CommentListResponse,
} from './types';
import axios from 'axios';
import { extractData } from '../../../shared/api/types/response';

export const commentApi = {
  // 댓글 목록 (페이지네이션)
  // GET /api/comments/post/{postId}/page?page=&size=
  getComments: async (
    { postId, page = 0, size = 10 }: GetCommentsParams
  ): Promise<CommentListResponse> => {
    try {
      const res = await apiClient.get<CommentListResponse>(
        `/api/comments/post/${postId}/page`,              // ✅ /page 추가
        { params: { page, size } }
      );

      // apiClient는 ApiResponse<T>를 반환하니까 res 자체가 ApiResponse<T>
      const data = extractData<CommentListResponse>(res);
      if (!data) throw new Error(res.message || '댓글 목록 조회 실패');

      return data; // { comments, totalCount, page, limit, hasMore }
    } catch (e) {
      // 서버가 "없음"을 404로 주면 빈 목록으로 매핑 (토스트 안 뜨게)
      if (axios.isAxiosError(e) && e.response?.status === 404) {
        return { comments: [], totalCount: 0, page, limit: size, hasMore: false };
      }
      throw e;
    }
  },

  // 댓글 생성
  // POST /api/comments
  createComment: async (payload: CreateCommentPayload): Promise<void> => {
    console.log(payload);
    const res = await apiClient.post<void>('/api/comments', payload);
    if (res.status !== 'SUCCESS') {
      throw new Error(res.message || '댓글 작성 실패');
    }
  },

  // 댓글 수정
  // PUT /api/comments/{id}
  updateComment: async (payload: UpdateCommentPayload): Promise<void> => {
    const res = await apiClient.put<void>(`/api/comments/${payload.commentId}`, {
      content: payload.content,
    });
    if (res.status !== 'SUCCESS') {
      throw new Error(res.message || '댓글 수정 실패');
    }
  },

  // 댓글 삭제
  // DELETE /api/comments/{id}
  deleteComment: async ({ commentId }: DeleteCommentPayload): Promise<void> => {
    const res = await apiClient.delete<void>(`/api/comments/${commentId}`);
    if (res.status !== 'SUCCESS') {
      throw new Error(res.message || '댓글 삭제 실패');
    }
  },
};

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

const unwrap = <T>(res: any) => (res?.data ?? res) as { status?: any; message?: string; data?: T };

export const commentApi = {
  // 댓글 목록 (페이지네이션)
  async getComments({ postId, page = 0, size = 10 }: GetCommentsParams): Promise<CommentListResponse> {
    try {
      const res = await apiClient.get<CommentListResponse>(`/api/comments/post/${postId}/page`, {
        params: { page, size },
      });
      const apiRes = unwrap<CommentListResponse>(res);
      // ApiResponse 형태면 extractData로 data만 뽑고, 아니면 그대로 사용
      const data = 'data' in apiRes ? extractData<CommentListResponse>(apiRes as any) : (apiRes as any);
      if (!data) throw new Error((apiRes as any).message ?? '댓글 목록 조회 실패');
      return data;
    } catch (e) {
      if (axios.isAxiosError(e)) {
        console.log('[getComments][AXIOS]', e.response?.status, e.response?.data);
        if (e.response?.status === 404) {
          return { comments: [], totalCount: 0, page, limit: size, hasMore: false };
        }
      } else {
        console.log('[getComments][ERROR]', e);
      }
      throw e;
    }
  },

  // 댓글 생성
  async createComment(payload: CreateCommentPayload): Promise<void> {
    try {
      console.log('[createComment][payload]', payload);
      const res = await apiClient.post<void>('/api/comments', payload);
      const apiRes = unwrap<void>(res);
      // 생성/수정/삭제는 data가 없을 수 있으니 status만 확인
      if (apiRes.status && apiRes.status !== 'SUCCESS') {
        throw new Error(apiRes.message ?? '댓글 작성 실패');
      }
      console.log('[createComment][OK]');
    } catch (e) {
      if (axios.isAxiosError(e)) {
        console.log('[createComment][AXIOS]', e.response?.status, e.response?.data);
      } else {
        console.log('[createComment][ERROR]', e);
      }
      throw e;
    }
  },

  // 댓글 수정
  async updateComment(payload: UpdateCommentPayload): Promise<void> {
    try {
      const res = await apiClient.put<void>(`/api/comments/${payload.commentId}`, { content: payload.content });
      const apiRes = unwrap<void>(res);
      if (apiRes.status && apiRes.status !== 'SUCCESS') {
        throw new Error(apiRes.message ?? '댓글 수정 실패');
      }
      console.log('[updateComment][OK]');
    } catch (e) {
      if (axios.isAxiosError(e)) {
        console.log('[updateComment][AXIOS]', e.response?.status, e.response?.data);
      } else {
        console.log('[updateComment][ERROR]', e);
      }
      throw e;
    }
  },

  // 댓글 삭제
  async deleteComment({ commentId }: DeleteCommentPayload): Promise<void> {
    try {
      const res = await apiClient.delete<void>(`/api/comments/${commentId}`);
      const apiRes = unwrap<void>(res);
      if (apiRes.status && apiRes.status !== 'SUCCESS') {
        throw new Error(apiRes.message ?? '댓글 삭제 실패');
      }
      console.log('[deleteComment][OK]');
    } catch (e) {
      if (axios.isAxiosError(e)) {
        console.log('[deleteComment][AXIOS]', e.response?.status, e.response?.data);
      } else {
        console.log('[deleteComment][ERROR]', e);
      }
      throw e;
    }
  },
};

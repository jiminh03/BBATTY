// entities/comment/api/api.ts
import { apiClient } from '../../../shared/api/client/apiClient';
import {
  CreateCommentPayload,
  UpdateCommentPayload,
  DeleteCommentPayload,
  GetCommentsParams,
  CommentListResponse,
} from './types';
import axios, {AxiosHeaders} from 'axios';
import { extractData } from '../../../shared/api/types/response';

// AxiosResponse | ApiResponse<T> 모두 커버해 실데이터 꺼내기
const unwrap = <T>(res: any): { message?: string; data?: T; status?: string } =>
  (res?.data ?? res) as any;

// 서버 응답을 CommentListResponse로 정규화 + 아이템 매핑
// entities/comment/api/api.ts
const normalizeList = (raw: any, page: number, size: number): CommentListResponse => {
  const rawComments: any[] = Array.isArray(raw?.comments) ? raw.comments : [];

  const comments = rawComments.map((c) => ({
    ...c,
    id: Number(c.id ?? c.commentId),
    authorId: c.authorId ?? c.userId,
    createdAt: c.createdAt ?? c.created_at ?? c.createAt,
    updatedAt: c.updatedAt ?? c.updated_at,
    isDeleted: Number(c.is_deleted ?? c.isDeleted ?? 0) === 1 || Boolean(c.isDeleted),
  }));

  const hasMore =
    typeof raw?.hasMore === 'boolean'
      ? raw.hasMore
      : typeof raw?.hasNext === 'boolean'
      ? raw.hasNext
      : comments.length === size;

  return {
    comments,
    totalCount: raw?.totalCount ?? raw?.total ?? comments.length ?? 0,
    page,
    limit: size,
    hasMore,
  };
};

export const commentApi = {
  // 댓글 목록
  async getComments({ postId, page = 0, size = 10 }: GetCommentsParams): Promise<CommentListResponse> {
    try {
      const res = await apiClient.get(`/api/comments/post/${postId}/page`, { params: { page, size } });
      const apiRes = unwrap<CommentListResponse>(res);
      const data = 'data' in apiRes ? extractData<any>(apiRes as any) : (apiRes as any);
      if (!data) throw new Error((apiRes as any).message ?? '댓글 목록 조회 실패');

      return normalizeList(data, page, size); // ✅ 정규화 후 반환
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
      const body = {
        ...payload,
        userId: Number(payload.userId),
        postId: Number(payload.postId),
        parentId: payload.parentId ?? null,
      };
      const res = await apiClient.post<void>('/api/comments', body);
      const apiRes = unwrap<void>(res);
      if (apiRes.status && apiRes.status !== 'SUCCESS') {
        throw new Error(apiRes.message ?? '댓글 작성 실패');
      }
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
    const res = await apiClient.put<void>(
      `/api/comments/${payload.commentId}`,
      payload.content, // ← 문자열 그대로
      {
        headers: new AxiosHeaders({
          'Content-Type': 'text/plain; charset=utf-8',
        }),
      }
    );
    const apiRes: any = (res as any)?.data ?? res;
    if (apiRes?.status && apiRes.status !== 'SUCCESS') {
      throw new Error(apiRes.message ?? '댓글 수정 실패');
    }
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
      const res = await apiClient.delete<void>(`/api/comments/${Number(commentId)}`);
      const apiRes: any = (res as any)?.data ?? res;
      if (apiRes.status && apiRes.status !== 'SUCCESS') {
        throw new Error(apiRes.message ?? '댓글 삭제 실패');
      }
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

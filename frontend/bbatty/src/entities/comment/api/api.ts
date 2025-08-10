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

// ApiResponse<T> | T 둘 다 허용 -> 항상 "실데이터" 형태로 뽑는 헬퍼
const unwrap = <T>(res: any): { message?: string; data?: T; status?: string } =>
  (res?.data ?? res) as any;

// 서버 응답이 다른 키를 쓸 수도 있으니 우리 타입으로 정규화
const normalizeList = (raw: any, page: number, size: number): CommentListResponse => {
  // 이미 우리가 원하는 형태면 그대로
  if (Array.isArray(raw?.comments) && typeof raw?.hasMore === 'boolean') {
    return raw as CommentListResponse;
  }

  // hasNext/nextCursor 같이 올 때 매핑
  if (Array.isArray(raw?.comments) && typeof raw?.hasNext === 'boolean') {
    return {
      comments: raw.comments,
      totalCount: raw.totalCount ?? 0,
      page,
      limit: size,
      hasMore: raw.hasNext,
    };
  }

  // page/totalPages 형태면 추론
  if (Array.isArray(raw?.comments) && typeof raw?.totalPages === 'number') {
    const hasMore = page + 1 < raw.totalPages;
    return {
      comments: raw.comments,
      totalCount: raw.totalCount ?? raw.total ?? 0,
      page,
      limit: size,
      hasMore,
    };
  }

  // 그래도 못 맞추면 안전한 기본값
  return {
    comments: raw?.comments ?? [],
    totalCount: raw?.totalCount ?? 0,
    page,
    limit: size,
    hasMore: (raw?.comments?.length ?? 0) === size,
  };
};

export const commentApi = {
  // 댓글 목록 (페이지네이션)
  async getComments({ postId, page = 0, size = 10 }: GetCommentsParams): Promise<CommentListResponse> {
    try {
      const res = await apiClient.get<CommentListResponse>(`/api/comments/post/${postId}/page`, {
        params: { page, size },
      });

      const apiRes = unwrap<CommentListResponse>(res);
      // ApiResponse 형태면 extractData로 data만 뽑고, 아니면 그대로 사용
      const data = 'data' in apiRes ? extractData<any>(apiRes as any) : (apiRes as any);

      if (!data) throw new Error((apiRes as any).message ?? '댓글 목록 조회 실패');

      // 👇 응답을 우리의 CommentListResponse로 정규화
      return normalizeList(data, page, size);
    } catch (e) {
      if (axios.isAxiosError(e)) {
        console.log('[getComments][AXIOS]', e.response?.status, e.response?.data);
        if (e.response?.status === 404) {
          // 비어있는 글은 404 줄 수도 있으니 빈 목록으로
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
      // userId, postId는 숫자여야 함(혹시 몰라 강제 변환)
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

  // 댓글 수정 (UI에선 string id를 넘겨도 됨 → 여기서 number로 변환)
  async updateComment(payload: UpdateCommentPayload & { commentId: string }): Promise<void> {
    try {
      const id = Number(payload.commentId);
      if (!Number.isFinite(id)) throw new Error('잘못된 댓글 ID');

      const res = await apiClient.put<void>(`/api/comments/${id}`, { content: payload.content });
      const apiRes = unwrap<void>(res);
      if (apiRes.status && apiRes.status !== 'SUCCESS') {
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

  // 댓글 삭제 (UI에선 string id를 넘겨도 됨 → 여기서 number로 변환)
  async deleteComment({ commentId }: DeleteCommentPayload & { commentId: string }): Promise<void> {
    try {
      const id = Number(commentId);
      if (!Number.isFinite(id)) throw new Error('잘못된 댓글 ID');

      const res = await apiClient.delete<void>(`/api/comments/${id}`);
      const apiRes = unwrap<void>(res);
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

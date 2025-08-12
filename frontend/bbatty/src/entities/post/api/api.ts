import { apiClient, ApiResponse, ApiErrorResponse, extractData } from '../../../shared/api';
import {
  CreatePostPayload,
  UpdatePostPayload,
  PresignedUrlPayload,
  PresignedUrlResponse,
  GetPostsParams,
  PostListItem,
} from './types';
import axios, { AxiosHeaders, AxiosResponse } from 'axios';
import { Post } from '../model/types';
import { CursorPostListResponse } from './types';

export const postApi = {
  // 게시글 생성
  createPost: (payload: CreatePostPayload): Promise<AxiosResponse<ApiResponse<Post>>> =>
    apiClient.post('/api/posts', payload),

  // 게시글 수정
  updatePost: async (
    postId: number,
    payload: { title: string; content: string; teamId?: number } // teamId까지 허용
  ) => {
    const body = {
      title: payload.title,
      content: payload.content,
      ...(payload.teamId != null ? { teamId: payload.teamId } : {}),
    };

    try {
      console.log('[updatePost] PUT /api/posts/%d body=', postId, body);
      const res = await apiClient.put<any>(`/api/posts/${postId}`, body);
      const apiRes: any = (res as any)?.data ?? res;
      const data = 'data' in apiRes ? extractData<any>(apiRes) : apiRes;
      console.log('[updatePost][OK]', data);
      return data;
    } catch (e) {
      if (axios.isAxiosError(e)) {
        console.log('[updatePost][AXIOS]', e.response?.status, e.response?.data);
      } else {
        console.log('[updatePost][ERROR]', e);
      }
      throw e;
    }
  },
    
  // 게시글 삭제
  deletePost: (postId: string) => apiClient.delete(`/api/posts/${postId}`),

  // 게시글 상세 조회
  getPostById: async (postId: number): Promise<Post> => {
  const res = await apiClient.get(`/api/posts/${postId}`);
  const api = (res as any).data as { status: string; message?: string; data?: Post };
  if (api?.status !== 'SUCCESS' || !api?.data) {
    throw new Error(api?.message ?? '게시글 상세 조회 실패');
  }
  return api.data;                 // ✅ Post
},

  // 게시글 목록 조회 (cursor 기반)
getPosts: async (teamId: number, cursor?: number): Promise<CursorPostListResponse> => {
  const params: Record<string, any> = {};
  if (cursor !== undefined) params.cursor = cursor;

  const res = await apiClient.get(`/api/posts/team/${teamId}`, { params });
  const api = (res as any).data as { status: string; message?: string; data?: any };

  if (api?.status !== 'SUCCESS' || !api?.data) {
    throw new Error(api?.message ?? '게시글 목록 조회 실패');
  }

  const raw = api.data;
  // ✅ 서버가 nextCursor를 string으로 주거나 누락해도 대비
  const normNextCursor =
    typeof raw?.nextCursor === 'string'
      ? Number(raw.nextCursor)
      : (typeof raw?.nextCursor === 'number' ? raw.nextCursor : undefined);

  return {
    posts: Array.isArray(raw?.posts) ? raw.posts : [],
    hasNext: Boolean(raw?.hasNext),
    nextCursor: normNextCursor,
  };
},


  // 인기 게시글 목록 조회
  async getPopularByTeam(teamId: number, limit = 20) {
    let cursor: number | undefined;
    const acc: PostListItem[] = [];
    const seen = new Set<number>();

    while (acc.length < limit) {
      const res = await apiClient.get(`/api/posts/team/${teamId}/popular`,
        { params: cursor !== undefined ? { cursor } : {} }
      );
      const api = (res as any).data as any;

      const page: PostListItem[] = Array.isArray(api.data)
        ? api.data
        : (api.data?.posts ?? []);

      console.log('[popular][page]', {
        got: page.length,
        hasNext: api.data?.hasNext,
        nextCursor: api.data?.nextCursor,
        acc: acc.length,
      });

      for (const p of page) {
        if (!seen.has(p.id)) {
          acc.push(p);
          seen.add(p.id);
          if (acc.length >= limit) break;
        }
      }

      const hasNext = !Array.isArray(api.data) && api.data?.hasNext === true;
      cursor = !Array.isArray(api.data) ? api.data?.nextCursor : undefined;
      if (!hasNext) break;
    }

    console.log('[popular][done] total=', acc.length);
    return acc.slice(0, limit);
  },


  // 게시글 좋아요
    likePost(postId: number) {
      return apiClient.post(`/api/posts/${postId}/like`); // 성공 코드만 내려옴
    },

  // 게시글 좋아요 취소
  unlikePost: (postId: string) => apiClient.delete(`/api/posts/${postId}/like`),

  // presigned-url 발급
  createPresignedUrl: (payload: PresignedUrlPayload) =>
    apiClient.post<PresignedUrlResponse>('/api/posts/presigned-url', payload),

  // 이미지 삭제
  deleteImage: (imageId: string) => apiClient.delete(`/api/images/${imageId}`),

  // 팀 별 게시글 검색
  async getTeamSearchPosts(
    teamId: number,
    keyword: string,
    cursor?: number
  ): Promise<CursorPostListResponse> {
    const res = await apiClient.get(`/api/posts/team/${teamId}/search`, {
      params: {
        keyword,                       // optional이지만 빈문자면 서버에서 전체검색 취급할 수도 있으니 trim 권장
        ...(cursor !== undefined ? { cursor } : {}),
      },
    });

    // 서버 공통 포맷 파싱 (SUCCESS / ERROR)
    const api = (res as any).data as {
      status: string; message?: string; data?: CursorPostListResponse;
    };

    if (api?.status !== 'SUCCESS' || !api?.data) {
      throw new Error(api?.message ?? '검색 실패');
    }
    return api.data; // { posts, hasNext, nextCursor }
  },

  // 팀별 검색 (query, cursor)
async searchTeamPosts(teamId: number, q: string, cursor?: number): Promise<CursorPostListResponse> {
  const query = (q ?? '').trim();
  if (!teamId || teamId <= 0 || !query) {
    return { posts: [], hasNext: false, nextCursor: undefined } as CursorPostListResponse;
  }
  const params: Record<string, any> = { query };
  if (cursor !== undefined) params.cursor = cursor;

  const res = await apiClient.get(`/api/posts/team/${teamId}/search`, { params });
  const root: any = (res as any)?.data ?? res;

  const payload = root?.data ?? root;
  const container = payload ?? root;

  const posts =
    Array.isArray(container?.posts) ? container.posts :
    Array.isArray(container) ? container : [];

  const hasNext = Boolean(container?.hasNext);
  const nextCursor = typeof container?.nextCursor === 'number'
    ? container.nextCursor
    : undefined;

  return { posts, hasNext, nextCursor } as CursorPostListResponse;
},
}

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
import { PostStatus } from '../model/types';

type UpdateBody = {
  postId: number;
  title: string;
  content: string;
  teamId?: number;
  status?: string;      // 서버가 postStatus 라면 키만 바꿔주면 됨
  postStatus?: string;  // <- 이 키가 맞다면 위 status 대신 이걸 쓰기
};

const clean = <T extends object>(obj: T): T =>
  Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== undefined)) as T;

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

  // 팀별 게시글 목록 조회 (cursor 기반)
  getPosts: async (teamId: number, cursor?: number): Promise<CursorPostListResponse> => {
    const res = await apiClient.get(`/api/posts/team/${teamId}`, {
      params: cursor !== undefined ? { cursor } : {},
    });

    // ✅ AxiosResponse → ApiResponse 형태로 직접 파싱
    const api = (res as any).data as {
      status: string;
      message?: string;
      data?: CursorPostListResponse;
    };

    if (api?.status !== 'SUCCESS' || !api?.data) {
      throw new Error(api?.message ?? '게시글 목록 조회 실패');
    }
    return api.data;                 // ✅ { posts, hasNext, nextCursor }
  },


  // 인기 게시글 목록 조회
  // api.ts
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

  // // 이미지 삭제
  deleteImage: (imageId: string) => apiClient.delete(`/api/images/${imageId}`),

};


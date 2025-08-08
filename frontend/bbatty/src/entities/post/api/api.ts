import { apiClient, ApiResponse, ApiErrorResponse, extractData } from '../../../shared/api';
import {
  CreatePostPayload,
  UpdatePostPayload,
  PresignedUrlPayload,
  PresignedUrlResponse,
  GetPostsParams,
  PostListItem,
} from './types';
import { AxiosHeaders, AxiosResponse } from 'axios';
import { Post } from '../model/types';
import { CursorPostListResponse } from './types';
import { PostStatus } from '../model/types';

export const postApi = {
  // 게시글 생성
  createPost: (payload: CreatePostPayload): Promise<AxiosResponse<ApiResponse<Post>>> =>
    apiClient.post('/api/posts', payload),

  // 게시글 수정
  updatePost: (postId: string, payload: UpdatePostPayload) => apiClient.put(`/api/posts/${postId}`, payload),

  // 게시글 삭제
  deletePost: (postId: string) => apiClient.delete(`/api/posts/${postId}`),

  // 게시글 상세 조회
  getPostById: async (postId: number): Promise<Post> => {
    // ⬇️ 여기서 제네릭은 "실데이터 타입"만!
    const res = await apiClient.get<Post>(`/api/posts/${postId}`);
    const data = extractData<Post>(res);  // ApiResponse<Post> → Post | null
    if (!data) throw new Error(res.message || '게시글 상세 조회 실패');
    return data;
  },

  // 팀별 게시글 목록 조회 (cursor 기반)
  getPosts: async (teamId: number, cursor?: number): Promise<CursorPostListResponse> => {
    const res = await apiClient.get<CursorPostListResponse>(
      `/api/posts/team/${teamId}`,
      { params: cursor !== undefined ? { cursor } : {} }
    );
    const data = extractData<CursorPostListResponse>(res);
    if (!data) throw new Error(res.message || '게시글 목록 조회 실패');
    return data; // 타입 OK
  },


  // 인기 게시글 목록 조회
  getPopularPosts: ({ page = 0, size = 10, teamId }: GetPostsParams) =>
    apiClient.get('/api/posts/popular', {
      headers: new AxiosHeaders(),
      params: { page, size, teamId },
    }),

  // 게시글 좋아요
  likePost: (postId: string) => apiClient.post(`/api/posts/${postId}/like`),

  // 게시글 좋아요 취소
  unlikePost: (postId: string) => apiClient.delete(`/api/posts/${postId}/like`),

  // presigned-url 발급
  createPresignedUrl: (payload: PresignedUrlPayload) =>
    apiClient.post<PresignedUrlResponse>('/api/posts/presigned-url', payload),

  // // 이미지 삭제
  // deleteImage: (imageId: string) => apiClient.delete(`/api/images/${imageId}`),

};


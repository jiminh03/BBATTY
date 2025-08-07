import { apiClient, ApiResponse, ApiErrorResponse } from '../../../shared/api';
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

function isSuccess<T>(res: ApiResponse<T> | ApiErrorResponse): res is ApiResponse<T> {
  return res.success === true;
}

export const postApi = {
  // 게시글 생성
  createPost: (payload: CreatePostPayload): Promise<AxiosResponse<ApiResponse<Post>>> =>
    apiClient.post('/api/posts', payload),

  // 게시글 수정
  updatePost: (postId: string, payload: UpdatePostPayload) => apiClient.put(`/api/posts/${postId}`, payload),

  // 게시글 삭제
  deletePost: (postId: string) => apiClient.delete(`/api/posts/${postId}`),

  // 게시글 상세 조회
  getPostById: async (postId: number) => {
      const res = await apiClient.get<ApiResponse<Post>>(`/api/posts/${postId}`)

      if (!res.data.success) {
        throw new Error(res.data.message || '게시글 상세 조회 실패')
      }

      return res.data.data
    },
  
  // 팀별 게시글 목록 조회
  getPosts : async (
    teamId: number,
    cursor?: number
  ): Promise<CursorPostListResponse> => {
    const token = localStorage.getItem('accessToken');

    const config = {
      params: cursor !== undefined ? { cursor } : {},
      headers: new AxiosHeaders(token ? { Authorization: `Bearer ${token}` } : {}),
    };

    const res = await apiClient.get<CursorPostListResponse>(
      `/api/posts/team/${teamId}`,
      config
    );

    const { status, message, data } = res.data;

    if (status !== 'SUCCESS' || !data) {
      throw new Error(message || '게시글 목록 조회 실패');
    }

    return data;
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


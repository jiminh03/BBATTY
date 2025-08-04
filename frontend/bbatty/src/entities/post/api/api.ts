import { apiClient } from '../../../shared/api';
import {
  CreatePostPayload,
  UpdatePostPayload,
  PresignedUrlPayload,
  PresignedUrlResponse,
  GetPostsParams,
  Post,
} from './types';
import { AxiosHeaders } from 'axios';

export const postApi = {
  // 게시글 생성
  createPost: (payload: CreatePostPayload) =>
    apiClient.post('/api/posts', payload),

  // 게시글 수정
  updatePost: (postId: string, payload: UpdatePostPayload) =>
    apiClient.put(`/api/posts/${postId}`, payload),

  // 게시글 삭제
  deletePost: (postId: string) =>
    apiClient.delete(`/api/posts/${postId}`),

  // 게시글 상세 조회
  getPostById: (postId: string) =>
    apiClient.get<Post>(`/api/posts/${postId}`),

  // 전체 게시글 목록 조회
  getPosts: ({ page = 0, size = 10, teamId }: GetPostsParams) =>
    apiClient.get('/api/posts', {
      headers: new AxiosHeaders(),
      params: { page, size, teamId },
    }),

  // 인기 게시글 목록 조회
  getPopularPosts: ({ page = 0, size = 10, teamId }: GetPostsParams) =>
    apiClient.get('/api/posts/popular', {
      headers: new AxiosHeaders(),
      params: { page, size, teamId },
    }),

  // 게시글 좋아요
  likePost: (postId: string) =>
    apiClient.post(`/api/posts/${postId}/like`),

  // 게시글 좋아요 취소
  unlikePost: (postId: string) =>
    apiClient.delete(`/api/posts/${postId}/like`),

  // presigned-url 발급
  createPresignedUrl: (payload: PresignedUrlPayload) =>
    apiClient.post<PresignedUrlResponse>('/api/posts/presigned-url', payload),
};

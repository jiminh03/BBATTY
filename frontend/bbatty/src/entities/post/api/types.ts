import { BaseEntity } from '../../../shared/api/types/common';

export interface Post extends BaseEntity {
  title: string;
  content: string;
  authorId: string;
  authorNickname: string;
  authorProfileImage?: string;
  likes: number;
  views: number;
  commentCount: number;
  isLiked?: boolean;
  tags?: string[];
  status: PostStatus;
  images?: string[];
}

export interface CreatePostPayload {
  title: string;
  content: string;
  tags?: string[];
  images?: string[];
}

export interface UpdatePostPayload {
  title?: string;
  content?: string;
  tags?: string[];
  images?: string[];
}

export interface PresignedUrlPayload {
  fileName: string;
  fileType: string;
}

export interface PresignedUrlResponse {
  url: string;
  key: string;
}

export enum PostStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED',
  DELETED = 'DELETED',
}

export enum PostSortType {
  LATEST = 'LATEST',
  POPULAR = 'POPULAR',
  MOST_LIKED = 'MOST_LIKED',
  MOST_VIEWED = 'MOST_VIEWED',
}

export interface PostFilters {
  search?: string;
  tags?: string[];
  sortType: PostSortType;
  authorId?: string;
}

export interface GetPostsParams {
  page?: number;
  size?: number;
  teamId?: string;
}

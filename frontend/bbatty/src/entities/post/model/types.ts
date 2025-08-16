// entities/post/model/types.ts
import { BaseEntity } from '../../../shared/api/types/common'

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

export interface Post extends BaseEntity {
  title: string;
  content: string;
  authorId: string;
  authorNickname: string;
  authorProfileImage?: string;

  // 기본 필드
  likes: number;
  views: number;
  commentCount: number;
  isLiked?: boolean;

  // ✅ 서버에서 가끔 내려오는 별칭(옵셔널)
  likeCount?: number;
  viewCount?: number;
  commentsCount?: number;
  likedByMe?: boolean;
  liked?: boolean;
  commentCnt?:number;

  // 기타
  tags?: string[];
  status: PostStatus;
  images?: string[];
  teamId?: number; // 목록/상세 전파 시 종종 필요
}

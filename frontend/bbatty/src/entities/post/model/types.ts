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
  title: string
  content: string
  authorId: string
  authorNickname: string
  authorProfileImage?: string
  likes: number
  views: number
  commentCount: number
  isLiked?: boolean
  tags?: string[]
  status: PostStatus
  images?: string[]
}

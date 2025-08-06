// entities/post/api/types.ts
import { PostSortType } from '../../post/model/types'

export interface CreatePostPayload {
  title: string
  content: string
  teamId: number
  isSameTeam?: boolean
  tags?: string[]
  images?: string[]
}

export interface UpdatePostPayload {
  title?: string
  content?: string
  tags?: string[]
  images?: string[]
}

export interface PresignedUrlPayload {
  fileName: string
  fileType: string
}

export interface PresignedUrlResponse {
  url: string
  key: string
  imageId: string
  imageUrl: string
}

export interface PostFilters {
  search?: string
  tags?: string[]
  sortType: PostSortType
  authorId?: string
}

export interface GetPostsParams {
  page?: number
  size?: number
  teamId?: string
}

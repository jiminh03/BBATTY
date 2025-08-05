import { Comment } from '../model/types'

export interface CreateCommentPayload {
  postId: string
  content: string
  parentId?: string | null
  depth?: number
}

export interface UpdateCommentPayload {
  commentId: string
  content: string
}

export interface DeleteCommentPayload {
  commentId: string
}

export interface GetCommentsParams {
  postId: string
  page?: number
  size?: number
}

export interface CommentListResponse {
  comments: Comment[]
  totalCount: number
  page: number
  limit: number
  hasMore: boolean
}

import { Comment } from '../model/types'

export interface CreateCommentPayload {
  postId: number
  userId: number
  content: string
  parentId?: string | null
}

export interface UpdateCommentPayload {
  commentId: string
  content: string
}

export interface DeleteCommentPayload {
  commentId: string
}

export interface GetCommentsParams {
  postId: number
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

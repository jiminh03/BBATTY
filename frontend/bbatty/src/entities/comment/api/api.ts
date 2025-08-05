import { apiClient } from '../../../shared/api/client/apiClient'
import {
  CreateCommentPayload,
  UpdateCommentPayload,
  DeleteCommentPayload,
  GetCommentsParams,
  CommentListResponse,
} from './types'
import { AxiosResponse } from 'axios'
import { ApiResponse, isSuccessResponse } from '../../../shared/api/types/response'
import { InternalAxiosRequestConfig } from 'axios'


export const commentApi = {
  getComments: async ({ postId, page = 0, size = 10 }: GetCommentsParams) => {
  const config = {
    params: { page, size },
  } as InternalAxiosRequestConfig

  const res = await apiClient.get<
    CommentListResponse,
    AxiosResponse<ApiResponse<CommentListResponse>>
  >(`/api/comments/post/${postId}`, config)

  if (!isSuccessResponse(res.data)) {
    throw new Error(res.data.message || '댓글 목록 조회 실패')
  }

  return res.data.data
}, 

  createComment: async (payload: CreateCommentPayload) => {
    const res = await apiClient.post<
      void,
      AxiosResponse<ApiResponse<void>>
    >(`/api/comments`, payload)

    if (!isSuccessResponse(res.data)) {
      throw new Error(res.data.message || '댓글 작성 실패')
    }
    return res.data.data
  },

  updateComment: async (payload: UpdateCommentPayload) => {
    const res = await apiClient.put<
      void,
      AxiosResponse<ApiResponse<void>>
    >(`/api/comments/${payload.commentId}`, {
      content: payload.content,
    })

    if (!isSuccessResponse(res.data)) {
      throw new Error(res.data.message || '댓글 수정 실패')
    }

    return res.data.data
  },

  deleteComment: async ({ commentId }: DeleteCommentPayload) => {
    const res = await apiClient.delete<
      void,
      AxiosResponse<ApiResponse<void>>
    >(`/api/comments/${commentId}`)

    if (!isSuccessResponse(res.data)) {
      throw new Error(res.data.message || '댓글 삭제 실패')
    }

    return res.data.data
  },
}

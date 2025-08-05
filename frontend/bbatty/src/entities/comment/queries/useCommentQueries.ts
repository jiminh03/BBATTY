// comment/queries/useCommentQueries.ts

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { commentApi } from '../api/api'
import {
  CreateCommentPayload,
  DeleteCommentPayload,
  UpdateCommentPayload,
  GetCommentsParams,
  CommentListResponse,
} from '../api/types'

export const useComments = (params: GetCommentsParams) => {
  return useQuery<CommentListResponse>({
    queryKey: ['comments', params.postId],
    queryFn: () => commentApi.getComments(params),
  })
}

export const useCreateComment = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreateCommentPayload) =>
      commentApi.createComment(payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['comments', variables.postId] })
    },
  })
}

export const useUpdateComment = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: UpdateCommentPayload) =>
      commentApi.updateComment(payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['comments'] })
    },
  })
}

export const useDeleteComment = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: DeleteCommentPayload) =>
      commentApi.deleteComment(payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['comments'] })
    },
  })
}

// post/queries/usePostQueries.ts

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { postApi } from '../api/api'
import { CreatePostPayload } from '../api/types'

export const useCreatePost = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreatePostPayload) => postApi.createPost(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['posts'] }) // 예시
    },
    onError: () => {
      console.error('게시글 생성 실패')
    },
  })
}


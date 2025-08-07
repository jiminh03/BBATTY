// post/queries/usePostQueries.ts

import { useMutation, useQuery, useQueryClient, useInfiniteQuery } from '@tanstack/react-query'
import { postApi } from '../api/api'
import { CreatePostPayload } from '../api/types'
import { CursorPostListResponse } from '../api/types'

export const usePostListQuery = () => {
  return useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts'],
    queryFn: ({ pageParam = undefined }) =>
      postApi.getPosts(pageParam as number | undefined),

    initialPageParam: undefined,

    getNextPageParam: (lastPage) =>
      lastPage.hasNext ? lastPage.nextCursor : undefined,
  });
};

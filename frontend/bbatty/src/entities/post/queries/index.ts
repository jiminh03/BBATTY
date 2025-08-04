import { useQuery } from '@tanstack/react-query';
import { postApi } from '../api/api';
import { PostFilters } from '../model/types';

export const usePostListQuery = (
  filters: PostFilters,
  page: number,
  size: number,
  teamId?: string
) => {
  return useQuery({
    queryKey: ['posts', filters, page, size, teamId],
    queryFn: () =>
      postApi.getPosts({
        page,
        size,
        teamId,
        // filters 안에 page/size/teamId 중복이 없다면 spread 가능
        ...filters,
      }),
    staleTime: 1000 * 60,
  });
};

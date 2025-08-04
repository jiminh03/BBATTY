import { useState, useCallback } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';
import type { InfiniteScrollParams, InfiniteScrollResponse } from '../types/infiniteScroll';
import { InfiniteScrollBuilder } from '../types/infiniteScroll';
import type { QueryKey } from '../lib/tanstack/queryKeys';
import type { ApiResponse } from '../types/response';

type InfiniteScrollApiFunction<TData> = (
  params: InfiniteScrollParams & { page: number }
) => Promise<ApiResponse<InfiniteScrollResponse<TData>>>;

interface UseInfiniteScrollOptions<TData> {
  queryKey: QueryKey;
  apiFunction: InfiniteScrollApiFunction<TData>;
  initialParams?: Partial<InfiniteScrollParams>;
  enabled?: boolean;
}

interface UseInfiniteScrollResult<TData> {
  data: TData[];
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  isFetching: boolean;
  isFetchingNextPage: boolean;
  hasNextPage: boolean;
  totalItems: number;
  fetchNextPage: () => void;
  refresh: () => void;
  changeLimit: (limit: number) => void;
  changeSorting: (sortBy: string) => void;
}

export const useInfiniteScroll = <TData>({
  queryKey,
  apiFunction,
  initialParams = {},
  enabled = true,
}: UseInfiniteScrollOptions<TData>): UseInfiniteScrollResult<TData> => {
  // 파라미터 상태
  const [params, setParams] = useState<InfiniteScrollParams>(() =>
    InfiniteScrollBuilder.createParams(initialParams.limit, initialParams.sortBy)
  );

  // 무한 쿼리
  const { data, isLoading, isError, error, isFetching, isFetchingNextPage, hasNextPage, fetchNextPage, refetch } =
    useInfiniteQuery({
      queryKey: [...queryKey, params],
      queryFn: async ({ pageParam = 1 }) => {
        const response = await apiFunction({ ...params, page: pageParam });
        return response;
      },
      initialPageParam: 1,
      getNextPageParam: (lastPage, pages) => {
        if (lastPage.success && lastPage.data.pagination.hasNext) {
          return pages.length + 1;
        }
        return undefined;
      },
      enabled,
    });

  // 모든 아이템 합치기
  const allItems: TData[] = [];
  let totalItems = 0;

  if (data?.pages) {
    data.pages.forEach((page) => {
      if (page.success && page.data) {
        allItems.push(...page.data.items);
        totalItems = page.data.pagination.totalItems;
      }
    });
  }

  // 액션 함수들
  const handleFetchNextPage = useCallback(() => {
    // 다음 페이지 로드 (스크롤 끝에 도달했을 때 호출)
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const changeLimit = useCallback((limit: number) => {
    // 페이지당 아이템 수 변경 (20개씩, 50개씩 보기 등)
    setParams((prev) => ({ ...prev, limit }));
  }, []);

  const changeSorting = useCallback((sortBy: string) => {
    // 정렬 기준 변경 (최신순, 인기순 등)
    setParams((prev) => ({ ...prev, sortBy }));
  }, []);

  const refresh = useCallback(() => {
    // 전체 데이터 새로고침 (Pull to Refresh)
    refetch();
  }, [refetch]);

  return {
    data: allItems,
    isLoading,
    isError,
    error: error as Error | null,
    isFetching,
    isFetchingNextPage,
    hasNextPage: hasNextPage || false,
    totalItems,
    fetchNextPage: handleFetchNextPage,
    refresh,
    changeLimit,
    changeSorting,
  };
};

import { useCallback } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';
import type {
  CursorScrollParams,
  CursorScrollResponse,
  CursorScrollOptions,
  CursorScrollResult,
} from '../types/cursorScroll';

export const useInfiniteCursorScroll = <TData, TParams extends CursorScrollParams>({
  queryKey,
  apiFunction,
  initialParams = {} as Omit<TParams, 'cursor'>,
  enabled = true,
  staleTime = 5 * 60 * 1000, // 5분
  gcTime = 10 * 60 * 1000, // 10분
}: CursorScrollOptions<TData, TParams>): CursorScrollResult<TData> => {
  // 무한 쿼리
  const {
    data,
    isLoading,
    isError,
    error,
    isFetching,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
    refetch,
  } = useInfiniteQuery({
    queryKey: [...queryKey, initialParams],
    queryFn: async ({ pageParam = null }) => {
      const params = {
        ...initialParams,
        cursor: pageParam,
      } as TParams;

      return await apiFunction(params);
    },
    initialPageParam: null,
    getNextPageParam: (lastPage) => {
      return lastPage.hasMore ? lastPage.nextCursor : undefined;
    },
    enabled,
    staleTime,
    gcTime,
  });

  // 모든 페이지의 데이터를 하나의 배열로 합치기 (중복 제거)
  const allItems: TData[] = [];
  const seenIds = new Set<string>();
  
  if (data?.pages) {
    data.pages.forEach((page) => {
      page.data.forEach((item) => {
        // gameId 또는 id 필드로 중복 체크
        const id = (item as any).gameId || (item as any).id;
        const uniqueKey = id ? String(id) : JSON.stringify(item);
        
        if (!seenIds.has(uniqueKey)) {
          seenIds.add(uniqueKey);
          allItems.push(item);
        } else {
          console.log('🔄 [InfiniteScroll] 중복 데이터 제거:', uniqueKey);
        }
      });
    });
  }

  // 다음 페이지 로드 핸들러
  const handleFetchNextPage = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  // 전체 새로고침 핸들러
  const handleRefresh = useCallback(() => {
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
    fetchNextPage: handleFetchNextPage,
    refresh: handleRefresh,
  };
};
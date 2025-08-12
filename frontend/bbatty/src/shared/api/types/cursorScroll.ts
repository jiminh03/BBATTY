export interface CursorScrollParams {
  cursor?: number | string | null;
  limit?: number;
  [key: string]: any;
}

export interface CursorScrollResponse<T> {
  data: T[];
  nextCursor: number | string | null;
  hasMore: boolean;
}

export interface CursorScrollOptions<TData, TParams extends CursorScrollParams> {
  queryKey: readonly unknown[];
  apiFunction: (params: TParams) => Promise<CursorScrollResponse<TData>>;
  initialParams?: Omit<TParams, 'cursor'>;
  enabled?: boolean;
  staleTime?: number;
  gcTime?: number;
}

export interface CursorScrollResult<TData> {
  data: TData[];
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  isFetching: boolean;
  isFetchingNextPage: boolean;
  hasNextPage: boolean;
  fetchNextPage: () => void;
  refresh: () => void;
}
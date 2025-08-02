import { useQuery, useMutation, useQueryClient, UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
import { InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { apiClient } from '../client/apiClient';
import { QueryKeys, type QueryKey } from '../lib/tanstack/queryKeys';
import { extractData, isSuccessResponse, type ApiResponse, type ApiSuccessResponse } from '../types/response';

type ApiFunction<TData, TVariables = void> = (variablese: TVariables) => Promise<AxiosResponse<ApiResponse<TData>>>;

// useApi 훅 옵션 타입
interface UseApiOptions<TData, TError = Error, TVariables = void>
  extends Omit<UseQueryOptions<ApiResponse<TData>, TError, TData, QueryKey>, 'queryKey' | 'queryFn'> {
  queryKey: QueryKey;
  apiFunction: ApiFunction<TData, TVariables>;
  variables?: TVariables;
}

export const useApi = <TData, TError = Error, TVariables = void>({
  queryKey,
  apiFunction,
  variables,
  ...options
}: UseApiOptions<TData, TError, TVariables>) => {
  return useQuery<ApiResponse<TData>, TError, TData, QueryKey>({
    queryKey,
    queryFn: async () => {
      const response = await apiFunction(variables as TVariables);
      return response.data;
    },
    select: (data) => {
      if (!data.success || !data.data) {
        throw new Error('Invalid API response');
      }
      return data.data;
    },
    ...options,
  });
};

// API 뮤테이션 훅 옵션 타입
interface UseApiMutationOptions<TData, TError = Error, TVariables = void>
  extends UseMutationOptions<TData, TError, TVariables> {
  apiFunction: ApiFunction<TData, TVariables>;
}

// 범용 뮤테이션 훅
export const useApiMutation = <TData, TError = Error, TVariables = void>({
  apiFunction,
  ...options
}: UseApiMutationOptions<TData, TError, TVariables>) => {
  return useMutation<TData, TError, TVariables>({
    mutationFn: async (variables: TVariables) => {
      const response = await apiFunction(variables);
      const data = extractData(response.data);
      if (data === null) {
        throw new Error('API 응답에서 데이터를 추출할 수 없습니다.');
      }
      return data;
    },
    ...options,
  });
};

import { AxiosResponse } from 'axios';
import { wrapAsync, AsyncResult } from '../../utils/result';
import { ApiResponse } from '../types/response';

export interface ApiError {
  type: 'API_ERROR';
  message: string;
  code?: string;
  statusCode?: string;
  details?: unknown;
}

const createApiError = (message: string, code?: string, statusCode?: string, details?: unknown): ApiError => ({
  type: 'API_ERROR',
  message,
  code,
  statusCode,
  details,
});

export const wrapApiCall = <T>(apicall: () => Promise<AxiosResponse<ApiResponse<T>>>): AsyncResult<T, ApiError> => {
  return wrapAsync(
    async () => {
      const response = await apicall();
      const apiResponse = response.data;

      if (apiResponse.status === 'SUCCESS') {
        return apiResponse.data;
      } else {
        throw createApiError(
          apiResponse.message,
          apiResponse.error?.code,
          apiResponse.error?.code,
          apiResponse.error?.details
        );
      }
    },
    (error: any) => {
      
      // Axios 에러 또는 기타 에러 처리
      if (error.type === 'API_ERROR') {
        return error; // 이미 우리가 만든 ApiError
      }

      // Axios 에러인 경우
      if (error.response) {
        return createApiError(
          error.response.data?.message || '서버 오류가 발생했습니다.',
          error.response.data?.error?.code,
          error.response.status,
          error.response.data
        );
      }

      // 네트워크 에러 등
      return createApiError(error.message || '알 수 없는 오류가 발생했습니다.');
    }
  );
};

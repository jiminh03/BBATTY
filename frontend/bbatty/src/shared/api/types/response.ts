export interface ApiSuccessResponse<T = unknown> {
  status: 'SUCCESS';
  message: string;
  data: T;
  timestamp: string;
}

export interface ApiErrorResponse {
  status: 'ERROR';
  message: string;
  error: {
    code: string; // 에러 식별자 ex) AUTH_TOKEN_EXPIRED
    details?: unknown;
  };
  timestamp: string;
}

export type ApiResponse<T = unknown> = ApiSuccessResponse<T> | ApiErrorResponse;

//null or 데이터 반환
export const extractData = <T>(response: ApiResponse<T>): T | null => {
  return response.status === 'SUCCESS' ? response.data : null;
};

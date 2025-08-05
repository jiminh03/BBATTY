export interface ApiSuccessResponse<T = unknown> {
  success: true;
  message: string;
  data: T;
  timestamp: string;
}

export interface ApiErrorResponse {
  success: false;
  message: string;
  error: {
    code: string; // 에러 식별자 ex) AUTH_TOKEN_EXPIRED
    details?: unknown;
  };
  timestamp: string;
}

export type ApiResponse<T = unknown> = ApiSuccessResponse<T> | ApiErrorResponse;

export class ResponseBuilder {
  static success<T>(data: T, message = 'Success'): ApiSuccessResponse<T> {
    return {
      success: true,
      message,
      data,
      timestamp: new Date().toISOString(),
    };
  }

  static error(message: string, code = 'UNKNOWN_ERROR', details?: unknown): ApiErrorResponse {
    return {
      success: false,
      message,
      error: {
        code,
        details,
      },
      timestamp: new Date().toISOString(),
    };
  }
}

//null 및 데이터 반환
export const extractData = <T>(response: ApiResponse<T>): T | null => {
  return response.success ? response.data : null;
};

//에러 및 데이터 반환
// export const safeExtractData = <T>(response: ApiResponse<T>): T => {
//   if (!isSuccessResponse(response)) throw new Error(`${response.error.code} ${response.error.details}`);
//   return response.data;
// };

export const extractError = (response: ApiResponse): { message: string; code: string; details?: unknown } | null => {
  return !response.success
    ? {
        message: response.message,
        code: response.error.code,
        details: response.error.details,
      }
    : null;
};

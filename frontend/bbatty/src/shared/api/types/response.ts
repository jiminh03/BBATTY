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
    /*
      fields: [
        { field: "email", message: "이메일 형식이 올바르지 않습니다" },
        { field: "password", message: "비밀번호는 8자 이상이어야 합니다" }
      ]
     */
  };
  timestamp: string;
}

export type ApiResponse<T = unknown> = ApiSuccessResponse<T> | ApiErrorResponse;

export class ResponseBuilder {
  static success<T>(data: T, message = "Success"): ApiSuccessResponse<T> {
    return {
      success: true,
      message,
      data,
      timestamp: new Date().toISOString(),
    };
  }

  static error(
    message: string,
    code = "UNKNOWN_ERROR",
    details?: unknown
  ): ApiErrorResponse {
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

export const isSuccessResponse = <T>(
  response: ApiResponse<T>
): response is ApiSuccessResponse<T> => {
  return response.success === true;
};

export const isErrorResponse = (
  response: ApiResponse
): response is ApiErrorResponse => {
  return response.success === false;
};

export const extractData = <T>(response: ApiResponse<T>): T | null => {
  return isSuccessResponse(response) ? response.data : null;
};

export const extractError = (
  response: ApiResponse
): { message: string; code: string; details?: unknown } | null => {
  return isErrorResponse(response)
    ? {
        message: response.message,
        code: response.error.code,
        details: response.error.details,
      }
    : null;
};

export const ErrorCodes = {
  // 인증 관련 에러
  AUTH_PERMISSION_DENIED: "AUTH_PERMISSION_DENIED",

  // 사용자 관련 에러
  USER_NOT_FOUND: "USER_NOT_FOUND",

  // 서버 관련 에러
  SERVER_ERROR: "SERVER_ERROR",
} as const;

export type ErrorCode = (typeof ErrorCodes)[keyof typeof ErrorCodes];

export interface ApiError {
  code: ErrorCode;
  message: string;
  details?: unknown;
  statusCode?: number;
  timestamp: string;
}

//Record : 모든 키에 대해 값이 존재함을 타입 레벨에서 보장
export const ErrorMessages: Record<ErrorCode, string> = {
  [ErrorCodes.AUTH_PERMISSION_DENIED]: "권한이 없습니다.",

  [ErrorCodes.USER_NOT_FOUND]: "존재하지 않는 사용자입니다.",

  [ErrorCodes.SERVER_ERROR]: "서버 오류가 발생했습니다.",
};

export const createApiError = (
  code: ErrorCode,
  message?: string,
  details?: unknown,
  statusCode?: number
): ApiError => ({
  code,
  message: message || ErrorMessages[code] || "알 수 없는 오류가 발생했습니다.",
  details,
  statusCode,
  timestamp: new Date().toISOString(),
});

export const mapHttpStatusToErrorCode = (statusCode: number): ErrorCode => {
  const statusMap: Record<number, ErrorCode> = {
    403: ErrorCodes.AUTH_PERMISSION_DENIED,
  };

  return statusMap[statusCode] || ErrorCodes.SERVER_ERROR;
};

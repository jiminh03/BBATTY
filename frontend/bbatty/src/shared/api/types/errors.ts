export const ErrorCodes = {
  // 인증 관련 에러
  AUTH_TOKEN_EXPIRED: 'AUTH_TOKEN_EXPIRED',
  AUTH_TOKEN_INVALID: 'AUTH_TOKEN_INVALID',
  AUTH_PERMISSION_DENIED: 'AUTH_PERMISSION_DENIED',

  // 사용자 관련 에러
  USER_NOT_FOUND: 'USER_NOT_FOUND',

  // 데이터 관련 에러
  DATA_NOT_FOUND: 'DATA_NOT_FOUND',
  DATA_VALIDATION_FAILED: 'DATA_VALIDATION_FAILED',

  // 서버 관련 에러
  SERVER_ERROR: 'SERVER_ERROR',

  // 파일 관련 에러
  FILE_TOO_LARGE: 'FILE_TOO_LARGE',

  // 네트워크 관련 에러
  NETWORK_ERROR: 'NETWORK_ERROR',
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
  // 인증 관련 에러
  [ErrorCodes.AUTH_TOKEN_EXPIRED]: '로그인이 만료되었습니다. 다시 로그인해주세요.',
  [ErrorCodes.AUTH_TOKEN_INVALID]: '유효하지 않은 인증 정보입니다.',
  [ErrorCodes.AUTH_PERMISSION_DENIED]: '권한이 없습니다.',

  // 사용자 관련 에러
  [ErrorCodes.USER_NOT_FOUND]: '존재하지 않는 사용자입니다.',

  // 데이터 관련 에러
  [ErrorCodes.DATA_NOT_FOUND]: '요청하신 데이터를 찾을 수 없습니다.',
  [ErrorCodes.DATA_VALIDATION_FAILED]: '입력 정보가 올바르지 않습니다.',

  // 서버 관련 에러
  [ErrorCodes.SERVER_ERROR]: '서버 오류가 발생했습니다.',

  // 네트워크 관련 에러
  [ErrorCodes.NETWORK_ERROR]: '네트워크 연결을 확인해주세요.',

  // 파일 관련 에러
  FILE_TOO_LARGE: 'FILE_TOO_LARGE',
};

export const createApiError = (
  code: ErrorCode,
  message?: string,
  details?: unknown,
  statusCode?: number
): ApiError => ({
  code,
  message: message || ErrorMessages[code] || '알 수 없는 오류가 발생했습니다.',
  details,
  statusCode,
  timestamp: new Date().toISOString(),
});

export const mapHttpStatusToErrorCode = (statusCode: number): ErrorCode => {
  const statusMap: Record<number, ErrorCode> = {
    400: ErrorCodes.DATA_VALIDATION_FAILED,
    401: ErrorCodes.AUTH_TOKEN_INVALID,
    403: ErrorCodes.AUTH_PERMISSION_DENIED,
    404: ErrorCodes.DATA_NOT_FOUND,
    413: ErrorCodes.FILE_TOO_LARGE,
    500: ErrorCodes.SERVER_ERROR,
    502: ErrorCodes.SERVER_ERROR,
  };

  return statusMap[statusCode] || ErrorCodes.SERVER_ERROR;
};

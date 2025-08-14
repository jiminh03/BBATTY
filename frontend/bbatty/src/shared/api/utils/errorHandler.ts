import { Alert } from 'react-native';
import { AxiosError } from 'axios';
import { API_CONFIG } from '../client/config';
import { ErrorCodes, ErrorMessages, mapHttpStatusToErrorCode, type ErrorCode } from '../types/errors';

interface ProcessedError {
  type: 'response_error' | 'network_error' | 'setup_error';
  statusCode?: number;
  code: ErrorCode;
  message: string;
  details?: unknown;
  url?: string;
  method?: string;
}

export const handleApiError = (error: AxiosError): ProcessedError => {
  const processedError = processError(error);

  if (API_CONFIG.errors.logToConsole) {
    console.log(`흠 ... ${error} :  ${error.toJSON}`);
  }

  if (API_CONFIG.errors.showToast) {
    showErrorToUser(processedError);
  }

  return processedError;
};

// 상황에 따른 에러 처리
const processError = (error: AxiosError): ProcessedError => {
  if (error.response) {
    // 서버에서 에러 상태 코드 응답을 받음
    const statusCode = error.response.status;
    const responseData = error.response.data as any;

    return {
      type: 'response_error',
      statusCode,
      code: responseData?.error?.code || mapHttpStatusToErrorCode(statusCode),
      message: responseData?.message || ErrorMessages[mapHttpStatusToErrorCode(statusCode)],
      details: responseData?.error?.details || null,
      url: error.config?.url,
      method: error.config?.method?.toUpperCase(),
    };
  } else if (error.request) {
    // 요청을 보냈지만 응답을 받지 못함 ( 네트워크 에러 )
    return {
      type: 'network_error',
      code: ErrorCodes.NETWORK_ERROR,
      message: ErrorMessages[ErrorCodes.NETWORK_ERROR],
      details: error.message,
      url: error.config?.url,
      method: error.config?.method?.toUpperCase(),
    };
  } else {
    // 요청을 설정하는 중에 발생한 에러
    return {
      type: 'setup_error',
      code: ErrorCodes.SERVER_ERROR,
      message: error.message || ErrorMessages[ErrorCodes.SERVER_ERROR],
      details: error.stack,
    };
  }
};

// 사용자에게 에러 알림 표시
const showErrorToUser = (processedError: ProcessedError): void => {
  // 사용자에게 보여주지 않을 에러들
  const silentErrors: ErrorCode[] = [
    ErrorCodes.AUTH_TOKEN_EXPIRED, // 자동으로 로그인 페이지로 이동
  ];

  if (silentErrors.includes(processedError.code)) {
    return;
  }

  // 중요한 에러는 Alert로, 일반적인 에러는 Toast로 처리
  const criticalErrors: ErrorCode[] = [
    ErrorCodes.SERVER_ERROR,
    ErrorCodes.NETWORK_ERROR,
    ErrorCodes.AUTH_PERMISSION_DENIED,
  ];

  if (criticalErrors.includes(processedError.code)) {
    Alert.alert('오류 ', processedError.message);
  } else {
    // Toast 메시지 표시 (실제 구현에서는 Toast 라이브러리 사용)
    console.log(' Toast:', processedError.message);
  }
};

// 특정 에러 타입인지 확인하는 헬퍼 함수들
export class ErrorCheckers {
  static isRetryableError(error: ProcessedError): boolean {
    const retryableErrorList = [ErrorCodes.NETWORK_ERROR, ErrorCodes.SERVER_ERROR] as const;

    return retryableErrorList.includes(error.code as any);
  }
}

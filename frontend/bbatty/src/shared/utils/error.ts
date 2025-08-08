/**
 * 채팅 에러 처리 유틸리티
 */

export enum ChatErrorType {
  CONNECTION_FAILED = 'CONNECTION_FAILED',
  CONNECTION_LOST = 'CONNECTION_LOST', 
  AUTHENTICATION_FAILED = 'AUTHENTICATION_FAILED',
  MESSAGE_SEND_FAILED = 'MESSAGE_SEND_FAILED',
  ROOM_ACCESS_DENIED = 'ROOM_ACCESS_DENIED',
  NETWORK_ERROR = 'NETWORK_ERROR',
  SERVER_ERROR = 'SERVER_ERROR',
  TIMEOUT_ERROR = 'TIMEOUT_ERROR',
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

export enum ChatErrorSeverity {
  LOW = 'low',
  MEDIUM = 'medium', 
  HIGH = 'high',
  CRITICAL = 'critical'
}

export interface ChatError {
  type: ChatErrorType;
  message: string;
  userMessage: string;
  severity: ChatErrorSeverity;
  retryable: boolean;
  autoRetry: boolean;
  timestamp: string;
  context?: Record<string, any>;
}

/**
 * 백엔드 에러 메시지를 사용자 친화적 메시지로 변환
 */
export const getErrorMessage = (error: any): ChatError => {
  const timestamp = new Date().toISOString();
  
  // WebSocket 연결 에러
  if (error?.type === 'close' || error?.code >= 1000) {
    return {
      type: ChatErrorType.CONNECTION_LOST,
      message: error.reason || 'WebSocket connection closed',
      userMessage: '채팅 연결이 끊어졌습니다. 잠시 후 다시 시도됩니다.',
      severity: ChatErrorSeverity.MEDIUM,
      retryable: true,
      autoRetry: true,
      timestamp,
      context: { code: error.code, reason: error.reason }
    };
  }

  // 네트워크 에러
  if (error?.message?.includes('Network') || error?.code === 'NETWORK_ERROR') {
    return {
      type: ChatErrorType.NETWORK_ERROR,
      message: error.message,
      userMessage: '네트워크 연결을 확인해 주세요.',
      severity: ChatErrorSeverity.HIGH,
      retryable: true,
      autoRetry: false,
      timestamp,
    };
  }

  // 인증 실패
  if (error?.message?.includes('Unauthorized') || error?.status === 401) {
    return {
      type: ChatErrorType.AUTHENTICATION_FAILED,
      message: error.message,
      userMessage: '로그인이 필요합니다. 다시 로그인해 주세요.',
      severity: ChatErrorSeverity.HIGH,
      retryable: false,
      autoRetry: false,
      timestamp,
    };
  }

  // 채팅방 접근 거부
  if (error?.message?.includes('Access denied') || error?.status === 403) {
    return {
      type: ChatErrorType.ROOM_ACCESS_DENIED,
      message: error.message,
      userMessage: '채팅방에 참여할 수 없습니다. 참여 조건을 확인해 주세요.',
      severity: ChatErrorSeverity.MEDIUM,
      retryable: false,
      autoRetry: false,
      timestamp,
    };
  }

  // 서버 에러
  if (error?.status >= 500 || error?.message?.includes('Server')) {
    return {
      type: ChatErrorType.SERVER_ERROR,
      message: error.message,
      userMessage: '서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.',
      severity: ChatErrorSeverity.HIGH,
      retryable: true,
      autoRetry: true,
      timestamp,
    };
  }

  // 타임아웃 에러
  if (error?.message?.includes('timeout') || error?.code === 'TIMEOUT') {
    return {
      type: ChatErrorType.TIMEOUT_ERROR,
      message: error.message,
      userMessage: '요청 시간이 초과되었습니다. 다시 시도해 주세요.',
      severity: ChatErrorSeverity.MEDIUM,
      retryable: true,
      autoRetry: true,
      timestamp,
    };
  }

  // 메시지 전송 실패
  if (error?.type === 'MESSAGE_SEND' || error?.message?.includes('send')) {
    return {
      type: ChatErrorType.MESSAGE_SEND_FAILED,
      message: error.message,
      userMessage: '메시지 전송에 실패했습니다. 다시 시도해 주세요.',
      severity: ChatErrorSeverity.LOW,
      retryable: true,
      autoRetry: false,
      timestamp,
    };
  }

  // 연결 실패
  if (error?.message?.includes('connect') || error?.type === 'CONNECTION_ERROR') {
    return {
      type: ChatErrorType.CONNECTION_FAILED,
      message: error.message,
      userMessage: '채팅 서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.',
      severity: ChatErrorSeverity.HIGH,
      retryable: true,
      autoRetry: true,
      timestamp,
    };
  }

  // 기본 에러
  return {
    type: ChatErrorType.UNKNOWN_ERROR,
    message: error?.message || JSON.stringify(error),
    userMessage: '알 수 없는 오류가 발생했습니다. 문제가 지속되면 고객센터로 문의해 주세요.',
    severity: ChatErrorSeverity.MEDIUM,
    retryable: true,
    autoRetry: false,
    timestamp,
  };
};

/**
 * 재시도 지연 시간 계산 (지수 백오프)
 */
export const calculateRetryDelay = (attempt: number, baseDelay: number = 1000): number => {
  return Math.min(baseDelay * Math.pow(2, attempt), 30000); // 최대 30초
};

/**
 * 에러가 재시도 가능한지 확인
 */
export const canRetry = (error: ChatError, currentAttempt: number, maxAttempts: number = 5): boolean => {
  return error.retryable && currentAttempt < maxAttempts;
};

/**
 * 에러 로깅 (개발/프로덕션 환경에 따라 다르게 처리)
 */
export const logChatError = (error: ChatError, context?: any) => {
  const logData = {
    ...error,
    context: { ...error.context, ...context },
    userAgent: navigator.userAgent,
    url: window.location.href,
  };

  if (__DEV__) {
    console.error(`[Chat Error - ${error.severity}]`, logData);
  }

  // 프로덕션 환경에서는 실제 로깅 서비스로 전송
  // analytics.track('chat_error', logData);
};
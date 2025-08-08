import { Platform } from 'react-native';
import { AxiosRequestConfig } from 'axios';

interface TimeoutConfig {
  default: number;
  upload: number;
  // download: number;-
}

interface RetryConfig {
  maxAttempts: number;
  delay: number;
  exponentialBackoff: boolean;
}

interface CacheConfig {
  defaultTTL: number;
  maxSize: number;
}

interface UploadConfig {
  maxFileSize: number;
  allowedTypes: string[];
}

interface PaginationConfig {
  defaultLimit: number;
  maxLimit: number;
}

interface ErrorConfig {
  showToast: boolean;
  logToConsole: boolean;
  reportToService: boolean;
}

interface ApiConfig {
  baseURL: string;
  chatBaseURL: string;
  socketUrl: string;
  timeout: TimeoutConfig;
  retry: RetryConfig;
  headers: AxiosRequestConfig['headers']; // 이렇게
  cache: CacheConfig;
  upload: UploadConfig;
  pagination: PaginationConfig;
  errors: ErrorConfig;
}

export const API_CONFIG: ApiConfig = {
  baseURL:'http://i13a403.p.ssafy.io:8080/',// Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080', // 일반 API 포트
  chatBaseURL: Platform.OS === 'android' ? 'http://10.0.2.2:8084' : 'http://localhost:8084', // 채팅 API 포트
  socketUrl: Platform.OS === 'android' ? 'ws://10.0.2.2:8084' : 'ws://localhost:8084',

  timeout: {
    default: 3000, // 3초
    upload: 10000, // 파일 업로드는 10초
    // download: 30000, // 파일 다운로드는 30초
  },

  retry: {
    maxAttempts: 3, // 최대 재시도 횟수
    delay: 1000, // 재시도 간격 (밀리초)
    exponentialBackoff: true, // 지수적 백오프 사용 여부
  },

  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    'X-Platform': Platform.OS,
    'X-App-Version': '1.0.0',
  },

  cache: {
    defaultTTL: 5 * 60 * 1000, // 5분
    maxSize: 50, // 최대 캐시 항목 수
  },

  upload: {
    maxFileSize: 10 * 1024 * 1024, // 10MB
    allowedTypes: ['image/jpeg', 'image/png', 'image/webp'],
  },

  pagination: {
    defaultLimit: 20,
    maxLimit: 100,
  },

  errors: {
    showToast: true, // 에러 토스트 표시 여부
    logToConsole: __DEV__, // 콘솔 로그 출력 여부
    reportToService: !__DEV__, // 에러 리포팅 서비스 전송 여부
  },
};

/*
interface DebugConfig {
  enableRequestLogging: boolean;
  enableResponseLogging: boolean;
  enableNetworkInspector: boolean;
  mockMode: boolean;
}

export const DEBUG_CONFIG: DebugConfig = {
  enableRequestLogging: __DEV__,
  enableResponseLogging: __DEV__,
  enableNetworkInspector: __DEV__,
  mockMode: false, // 개발 중 목 데이터 사용 여부
};
*/

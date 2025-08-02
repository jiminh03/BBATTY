import { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { Alert } from 'react-native';
import { API_CONFIG /*, DEBUG_CONFIG*/ } from './config';
import { tokenManager } from './apiClient';
import { handleApiError } from '../utils/errorHandler';
import { retryRequest } from '../utils/retry';

// 요청 메타데이터
interface RequestMetadata {
  startTime: number;
  requestId: string;
}

// Axios 확장
declare module 'axios' {
  interface AxiosRequestConfig {
    metadata?: RequestMetadata;
    _retry?: boolean;
  }
}

export const setupInterceptors = (client: AxiosInstance): void => {
  // 요청 인터셉터
  client.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
      // 요청 메타데이터 추가
      const requestId = `req_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
      config.metadata = {
        startTime: Date.now(),
        requestId,
      };

      /*
      // 디버그 로깅
      if (DEBUG_CONFIG.enableRequestLogging) {
        console.log(`🚀 API Request [${requestId}]: ${config.method?.toUpperCase()} ${config.url}`, {
          params: config.params,
          data: config.data,
          headers: config.headers,
        });
      }
      */

      //퍼블릭 엔드포인트가 아닌 경우 토큰 추가
      const isPublicEndpoint = false; // config.url?.includes('/auth/login');

      if (!isPublicEndpoint) {
        const token = await tokenManager.getToken();
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      }

      if (config.headers) {
        config.headers['X-Request-ID'] = requestId;
      }

      return config;
    },
    (error: AxiosError) => {
      console.error('요청 interceptor error: ', error);
      return Promise.reject(error);
    }
  );

  // 응답 인터셉터
  client.interceptors.response.use(
    (response: AxiosResponse) => {
      //const duration = Date.now() - (response.config.metadata?.startTime || 0);
      const requestId = response.config.metadata?.requestId;

      /*
      // 디버그 로깅
      if (DEBUG_CONFIG.enableResponseLogging) {
        console.log(`✅ API Response [${requestId}]: ${response.config.method?.toUpperCase()} ${response.config.url}`, {
          status: response.status,
          duration: `${duration}ms`,
          data: response.data,
        });
      }
        */
      if (response.data && typeof response.data === 'object') {
        if (!('success' in response.data)) {
          console.warn(`잘못된 api 형식 [${requestId}]:`, response.data);
        }
      }

      return response;
    },
    async (error: AxiosError) => {
      const originalRequest = error.config;
      //const duration = Date.now() - (originalRequest?.metadata?.startTime || 0);
      //const requestId = originalRequest?.metadata?.requestId;

      /*
      // 디버그 로깅
      if (DEBUG_CONFIG.enableResponseLogging) {
        console.log(`✅ API Response [${requestId}]: ${response.config.method?.toUpperCase()} ${response.config.url}`, {
          status: response.status,
          duration: `${duration}ms`,
          data: response.data,
        });
      }
      */

      // 토큰 만료 또는 인증 실패
      if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
        originalRequest._retry = true;
        // 토큰 제거
        await tokenManager.removeToken();

        //  적절한 네비게이션 로직으로 대체해야 함
        return Promise.reject(error);
      }

      // 권한 부족
      if (error.response?.status === 403) {
        if (API_CONFIG.errors.showToast) {
          Alert.alert('접근 권한 없음', '이 기능을 사용할 권한이 없습니다.');
        }
        return Promise.reject(error);
      }

      // 서버 에러 - 재시도 로직 적용
      if (error.response?.status && error.response.status >= 500 && error.response.status < 600) {
        const shouldRetry = await retryRequest(originalRequest, error);
        if (shouldRetry && originalRequest) {
          return client(originalRequest);
        }
      }

      // 네트워크 에러 처리
      if (error.code === 'ECONNABORTED' || error.message === 'Network Error') {
        if (API_CONFIG.errors.showToast) {
          Alert.alert('네트워크 오류', '인터넷 연결을 확인해주세요.');
        }
      }

      // 에러 처리 및 로깅
      handleApiError(error);

      return Promise.reject(error);
    }
  );
};

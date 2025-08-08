import { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { Alert } from 'react-native';
import { tokenManager } from './tokenManager';
import { API_CONFIG } from './config';
import { handleApiError } from '../utils/errorHandler';
import { retryRequest } from '../utils/retry';

// 토큰 제거시 호출될 콜백
type OnUnauthorizedCallback = () => Promise<void>;

export const setupInterceptors = (client: AxiosInstance, onUnauthorized: OnUnauthorizedCallback): void => {
  // 요청 인터셉터
  client.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
      //퍼블릭 엔드포인트가 아닌 경우 토큰 추가
      const isPublicEndpoint = /\/api\/(auth\/(signup|check-nickname|refresh))(\/.*)?$/.test(config.url || '');

      if (!isPublicEndpoint) {
        const token = await tokenManager.getToken();
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }
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
      if (response.data && typeof response.data === 'object') {
        if (!('SUCCESS' in response.data)) {
          console.warn(`잘못된 api 형식 :`, response.data);
        }
      }
      return response;
    },
    async (error: AxiosError) => {
      const originalRequest = error.config;

      // 토큰 만료 또는 인증 실패
      if (error.response?.status === 401 && originalRequest) {
        // 토큰 제거
        await tokenManager.removeToken();

        // 콜백이 제공된 경우 실행 (클라이언트 헤더 업데이트 등)
        if (onUnauthorized) {
          await onUnauthorized();
        }

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

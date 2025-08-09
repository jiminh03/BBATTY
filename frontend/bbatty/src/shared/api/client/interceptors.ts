import { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { Alert } from 'react-native';
import { tokenManager } from './tokenManager';
import { API_CONFIG } from './config';
import { handleApiError } from '../utils/errorHandler';
import { retryRequest } from '../utils/retry';
import { authApi } from '../../../entities/auth/api/authApi';

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
          // 매치채팅 입장 시에만 전체 토큰 로깅
          if (config.url?.includes('/api/match-chat/') || config.url?.includes('/api/watch-chat/')) {
            console.log('매치채팅 API 요청 URL:', config.url);
            console.log('전체 JWT 토큰:', token);
            // 리프레시 토큰도 함께 로깅
            tokenManager.getRefreshToken().then(refreshToken => {
              console.log('리프레시 토큰:', refreshToken || 'null');
            });
          }
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
        if (!('status' in response.data) || response.data.status !== 'SUCCESS') {
          console.warn(`잘못된 api 형식 `, response.data);
        }
      }
      return response;
    },
    async (error: AxiosError) => {
      const originalRequest = error.config;

      // 토큰 만료 또는 인증 실패
      if (error.response?.status === 401 && originalRequest) {
        const isChatAPI = originalRequest.url?.includes('/api/match-chat/') || originalRequest.url?.includes('/api/watch-chat/');
        
        // Chat API의 경우 토큰 삭제하지 않고 에러만 전달
        // (상위에서 JWT 토큰 문제인지 판단하도록)
        if (isChatAPI) {
          console.warn('Chat API 401 에러 - JWT 토큰 확인 필요:', originalRequest.url);
          return Promise.reject(error);
        }
        
        // 일반 API는 기존 로직대로 토큰 제거 및 로그아웃 처리
        await tokenManager.removeToken();
        await tokenManager.removeRefreshToken();

        // 콜백이 제공된 경우 실행 (네비게이션 처리 등)
        if (onUnauthorized) {
          await onUnauthorized();
        }

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

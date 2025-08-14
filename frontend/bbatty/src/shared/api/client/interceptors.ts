import { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { Alert } from 'react-native';
import { useTokenStore } from '../token/tokenStore';
import { API_CONFIG } from './config';
import { handleApiError } from '../utils/errorHandler';
import { retryRequest } from '../utils/retry';
import { isOk } from '../../utils/result';

// 토큰 제거시 호출될 콜백
type OnUnauthorizedCallback = () => Promise<void>;

export const setupInterceptors = (client: AxiosInstance, onUnauthorized: OnUnauthorizedCallback): void => {
  // 요청 인터셉터
  client.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
      //퍼블릭 엔드포인트가 아닌 경우 토큰 추가
      const isPublicEndpoint = /\/api\/(auth\/(signup|check-nickname|refresh|login))(\/.*)?$/.test(config.url || '');

      if (!isPublicEndpoint) {
        const tokenStore = useTokenStore.getState();

        // 토큰 만료 사전 체크 및 필요시 갱신
        const refreshResult = await tokenStore.checkAndRefreshIfNeeded();

        if (!isOk(refreshResult)) {
          console.error('❌ [RequestInterceptor] 토큰 갱신 실패, 인증 초기화');
          await onUnauthorized();
          return Promise.reject(new Error('Token refresh failed'));
        }

        // 최신 토큰을 헤더에 추가
        const token = tokenStore.getAccessToken();
        if (token && config.headers) {
          console.log(token);
          config.headers.Authorization = `Bearer ${token}`;

          // console.log(`Bearer ${token}`);
        } else if (!token) {
          console.error('❌ [RequestInterceptor] 토큰이 없음, 인증 초기화');
          await onUnauthorized();
          return Promise.reject(new Error('No access token'));
        }
      }

      console.log(config.url, config.params);

      return config;
    },
    (error: AxiosError) => {
      console.error('❌ [RequestInterceptor] 요청 interceptor error: ', error);
      return Promise.reject(error);
    }
  );

  // 응답 인터셉터
  client.interceptors.response.use(
    (response: AxiosResponse) => {
      if (response.data && typeof response.data === 'object') {
        if (!('status' in response.data)) {
          console.warn(`⚠️ [ResponseInterceptor] 잘못된 api 형식 :`, response.data);
        }
      }
      return response;
    },
    async (error: AxiosError) => {
      const originalRequest = error.config;

      // 토큰 만료 또는 인증 실패 (백업 처리)
      if (error.response?.status === 401 && originalRequest) {
        console.log('🔴 [ResponseInterceptor] 401 에러 발생 - 백업 처리:', originalRequest?.url);

        // refresh API 호출은 별도 처리 (무한 루프 방지)
        const isRefreshRequest = originalRequest.url?.includes('/api/auth/refresh');

        if (isRefreshRequest) {
          console.error('❌ [ResponseInterceptor] Refresh API 자체가 401 에러, 로그아웃 처리');
          await onUnauthorized();
          return Promise.reject(error);
        }

        // 요청 인터셉터에서 사전 체크가 이루어지므로, 401은 예외 상황
        // 즉시 로그아웃 처리 (토큰 재발급 시도하지 않음)
        console.error('❌ [ResponseInterceptor] 요청 인터셉터 사전 체크 후에도 401 발생, 로그아웃 처리');
        await onUnauthorized();
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

      // // 네트워크 에러 처리
      // if (error.code === '' || error.message === 'Network Error') {
      //   if (API_CONFIG.errors.showToast) {
      //     Alert.alert('네트워크 오류', '인터넷 연결을 확인해주세요.');
      //   }
      // }

      // 에러 처리 및 로깅
      handleApiError(error);

      return Promise.reject(error);
    }
  );
};

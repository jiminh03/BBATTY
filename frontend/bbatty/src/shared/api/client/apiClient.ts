import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiResponse } from '../types/response';
import { API_CONFIG } from './config';
import { setupInterceptors, setInterceptorsDisabled } from './interceptors';
import { useTokenStore } from '../token/tokenStore';
import { isErr } from '../../utils/result';

interface CustomAxiosInstance extends AxiosInstance {
  get<T = unknown, R = AxiosResponse<ApiResponse<T>>, D = any>(
    url: string,
    config?: Partial<InternalAxiosRequestConfig<D>>
  ): Promise<R>;

  post<T = unknown, R = AxiosResponse<ApiResponse<T>>, D = any>(
    url: string,
    data?: D,
    config?: InternalAxiosRequestConfig<D>
  ): Promise<R>;

  put<T = unknown, R = AxiosResponse<ApiResponse<T>>, D = any>(
    url: string,
    data?: D,
    config?: InternalAxiosRequestConfig<D>
  ): Promise<R>;

  delete<T = unknown, R = AxiosResponse<ApiResponse<T>>, D = any>(
    url: string,
    config?: InternalAxiosRequestConfig<D>
  ): Promise<R>;
}

// 메인 클라 (일반 API용 - 8080 포트)
const apiClient: CustomAxiosInstance = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout.default,
  headers: API_CONFIG.headers,
}) as CustomAxiosInstance;

// 채팅 API용 클라 (8084 포트)
const chatApiClient: CustomAxiosInstance = axios.create({
  baseURL: API_CONFIG.chatBaseURL,
  timeout: API_CONFIG.timeout.default,
  headers: API_CONFIG.headers,
}) as CustomAxiosInstance;

// 파일 업로드용 클라
const uploadClient: CustomAxiosInstance = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout.upload,
  headers: {
    ...API_CONFIG.headers,
    'Content-Type': 'multipart/form-data',
  },
}) as CustomAxiosInstance;

const AUTHORIZATION = 'Authorization' as const;

// 토큰을 헤더에 적용
const applyTokenToClients = (token: string | null) => {
  if (token) {
    const authHeader = `Bearer ${token}`;
    apiClient.defaults.headers.common[AUTHORIZATION] = authHeader;
    chatApiClient.defaults.headers.common[AUTHORIZATION] = authHeader;
    uploadClient.defaults.headers.common[AUTHORIZATION] = authHeader;
  } else {
    delete apiClient.defaults.headers.common[AUTHORIZATION];
    delete chatApiClient.defaults.headers.common[AUTHORIZATION];
    delete uploadClient.defaults.headers.common[AUTHORIZATION];
  }
};

// 로그아웃 처리를 위한 콜백 저장
let onUnauthorizedCallback: (() => void) | null = null;

// 인터셉터 비활성화 플래그 (회원탈퇴 시 사용)
let interceptorsDisabled = false;

// 401 에러 시 호출될 콜백
const handleUnauthorized = async () => {
  // 인터셉터가 비활성화된 경우 처리하지 않음 (회원탈퇴 진행 중)
  if (interceptorsDisabled) {
    return;
  }
  
  await useTokenStore.getState().clearTokens();
  applyTokenToClients(null);
  // AppNavigator에서 설정한 콜백 호출 (인증 상태 변경)
  if (onUnauthorizedCallback) {
    onUnauthorizedCallback();
  }
};

export const initializeApiClient = async (): Promise<void> => {
  useTokenStore.getState().setApiClient(apiClient);

  // 저장된 토큰 초기화
  const initResult = await useTokenStore.getState().initializeTokens();
  if (isErr(initResult)) {
    console.error('❌ [ApiClient] 토큰 초기화 실패:', initResult.error);
  }

  const token = useTokenStore.getState().getAccessToken();
  applyTokenToClients(token);

  setupInterceptors(apiClient, handleUnauthorized);
  setupInterceptors(chatApiClient, handleUnauthorized);
  setupInterceptors(uploadClient, handleUnauthorized);
  
  console.log('✅ [ApiClient] API 클라이언트 초기화 완료');
};

// AppNavigator에서 호출할 함수
export const setUnauthorizedCallback = (callback: () => void) => {
  onUnauthorizedCallback = callback;
};

// 회원탈퇴 시 인터셉터 비활성화
export const disableInterceptors = () => {
  interceptorsDisabled = true;
  setInterceptorsDisabled(true); // 인터셉터에도 상태 전달
  applyTokenToClients(null); // 모든 토큰 헤더 제거
};

// 인터셉터 다시 활성화
export const enableInterceptors = () => {
  interceptorsDisabled = false;
  setInterceptorsDisabled(false); // 인터셉터에도 상태 전달
};

export { apiClient, chatApiClient, uploadClient };

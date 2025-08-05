import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiResponse } from '../types/response';
import { API_CONFIG } from './config';
import { setupInterceptors } from './interceptors';
import { tokenManager } from './tokenManager';

interface CustomAxiosInstance extends AxiosInstance {
  get<T = unknown, R = AxiosResponse<ApiResponse<T>>, D = any>(
    url: string,
    config?: InternalAxiosRequestConfig<D>
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

// 메인 클라
const apiClient: CustomAxiosInstance = axios.create({
  baseURL: API_CONFIG.baseURL,
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
    uploadClient.defaults.headers.common[AUTHORIZATION] = authHeader;
  } else {
    delete apiClient.defaults.headers.common[AUTHORIZATION];
    delete uploadClient.defaults.headers.common[AUTHORIZATION];
  }
};

// 401 에러 시 호출될 콜백
const handleUnauthorized = async () => {
  applyTokenToClients(null);
  // 추가적인 정리 작업이 필요한 경우 여기서 수행
};

export const initializeApiClient = async (): Promise<void> => {
  const token = await tokenManager.getToken();
  applyTokenToClients(token);

  setupInterceptors(apiClient, handleUnauthorized);
  setupInterceptors(uploadClient, handleUnauthorized);

  /*
  // 디버그 모드에서 초기화 로그
  if (DEBUG_CONFIG.enableRequestLogging) {
    console.log('초기화 완, baseURL :', API_CONFIG.baseURL);
  }
    */
};

// 토큰 변경 시 갱신
export const updateClientsToken = async (token: string | null) => {
  applyTokenToClients(token);
};

export { apiClient, uploadClient };

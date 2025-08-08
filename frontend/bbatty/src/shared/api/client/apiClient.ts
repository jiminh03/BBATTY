import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiResponse } from '../types/response';
import { API_CONFIG } from './config';
import { setupInterceptors } from './interceptors';
import { tokenManager } from './tokenManager';

interface CustomAxiosInstance extends AxiosInstance {
  get<T = unknown, R = ApiResponse<T>, D = any>(
    url: string,
    config?: Partial<InternalAxiosRequestConfig<D>>
  ): Promise<R>;

  post<T = unknown, R = ApiResponse<T>, D = any>(
    url: string,
    data?: D,
    config?: InternalAxiosRequestConfig<D>
  ): Promise<R>;

  put<T = unknown, R = ApiResponse<T>, D = any>(
    url: string,
    data?: D,
    config?: InternalAxiosRequestConfig<D>
  ): Promise<R>;

  delete<T = unknown, R = ApiResponse<T>, D = any>(url: string, config?: InternalAxiosRequestConfig<D>): Promise<R>;
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

// 401 에러 시 호출될 콜백
const handleUnauthorized = async () => {
  applyTokenToClients(null);
};

export const initializeApiClient = async (): Promise<void> => {
  const token = await tokenManager.getToken();
  applyTokenToClients(token);

  setupInterceptors(apiClient, handleUnauthorized);
  setupInterceptors(chatApiClient, handleUnauthorized);
  setupInterceptors(uploadClient, handleUnauthorized);
};

export { apiClient, chatApiClient, uploadClient };

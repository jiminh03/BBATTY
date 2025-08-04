import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import * as SecureStore from 'expo-secure-store';
import { ApiResponse } from '../types/response';
import { API_CONFIG } from './config';
import { setupInterceptors } from './interceptors';

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

// 파일 다운로드용 클라
const downloadClient = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout.download,
  responseType: 'blob',
});

// ========================== TokenManager =====================================
// 별도 파일로 분리하려다가 구조가 지저분해져서 통합

const AUTH_TOKEN = 'authToken' as const;
const AUTHORIZATION = 'Authorization' as const;

interface TokenManager {
  setToken(token: string): Promise<void>;
  getToken(): Promise<string | null>;
  removeToken(): Promise<void>;
  restoreToken(): Promise<void>;
}

export const tokenManager: TokenManager = {
  async setToken(token: string): Promise<void> {
    try {
      await SecureStore.setItemAsync(AUTH_TOKEN, token);
      // 모든 클라이언트에 토큰 적용
      // Bearer 토큰 => 토큰을 소유한 사람에게 액세스 권한을 부여하는 일반적인 토큰 클래스(액세스 토큰, ID 토큰, 자체 서명 JWT)를 뜻함
      const authHeader = `Bearer ${token}`;
      apiClient.defaults.headers.common[AUTHORIZATION] = authHeader;
      uploadClient.defaults.headers.common[AUTHORIZATION] = authHeader;
      downloadClient.defaults.headers.common[AUTHORIZATION] = authHeader;
    } catch (error) {
      console.warn('토큰을 저장하는데 실패하였습니다 : ', error);
      //throw하지 않으면 호출자가 실패를 모를 수 있어서 예상치 못한 버그가 발생 => 반환타입이 void라 별도 체크해줘야함
      throw new Error('토큰 저장 실패');
    }
  },

  async getToken(): Promise<string | null> {
    try {
      return await SecureStore.getItemAsync(AUTH_TOKEN);
    } catch (error) {
      console.warn('토큰을 불러오는데 실패하였습니다 : ', error);
      return null;
    }
  },

  async removeToken(): Promise<void> {
    try {
      await SecureStore.deleteItemAsync(AUTH_TOKEN);
      delete apiClient.defaults.headers.common[AUTHORIZATION];
      delete uploadClient.defaults.headers.common[AUTHORIZATION];
      delete downloadClient.defaults.headers.common[AUTHORIZATION];
    } catch (error) {
      console.warn('토큰 제거 실패', error);
    }
  },

  // 디바이스에 저장됐던 토큰 => 메모리로 복원
  async restoreToken(): Promise<void> {
    const token = await this.getToken();
    if (token) {
      await this.setToken(token);
    }
  },
};

export const initializeApiClient = async (): Promise<void> => {
  await tokenManager.restoreToken();

  setupInterceptors(apiClient);
  setupInterceptors(uploadClient);
  setupInterceptors(downloadClient);

  /*
  // 디버그 모드에서 초기화 로그
  if (DEBUG_CONFIG.enableRequestLogging) {
    console.log('초기화 완, baseURL :', API_CONFIG.baseURL);
  }
    */
};

export { apiClient, uploadClient, downloadClient };

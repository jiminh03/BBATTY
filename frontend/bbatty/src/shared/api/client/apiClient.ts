import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from "axios";
import { ApiResponse } from "../types/response";
import { API_CONFIG } from "./config";

// 커스텀 axios 인스턴스 타입
/*
interface CustomAxiosInstance extends AxiosInstance {
  get<T = unknown>(
    url: string,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<ApiResponse<T>>>;
}
*/

// 메인 API 클라이언트 생성
const apiClient = axios.create({
  baseURL: API_CONFIG.baseURL,
  timeout: API_CONFIG.timeout.default,
  //headers: API_CONFIG.headers,
});

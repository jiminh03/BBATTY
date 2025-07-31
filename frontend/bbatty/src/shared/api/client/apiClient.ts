import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from "axios";
import { ApiResponse } from "../types/response";

// 커스텀 axios 인스턴스 타입
interface CustomAxoisInstance extends AxiosInstance {
  /*
  get<T = unknown>(
    url: string,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<ApiResponse<T>>>;*/
}

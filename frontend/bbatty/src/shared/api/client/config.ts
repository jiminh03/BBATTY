import { Platform } from "react-native";
import { RuntimeEnvironment } from "../types/common";

interface TimeoutConfig {
  default: number;
  upload: number;
  download: number;
}

interface ApiConfig {
  baseURL: string;
  timeout: TimeoutConfig;
}

const API_URLS: Record<RuntimeEnvironment, string> = {
  development: "--",
  production: "--",
};

// 현재 환경 감지
const getCurrentEnvironment = (): RuntimeEnvironment => {
  if (__DEV__) return "development";
  // 실제 환경에서는 빌드 설정이나 환경 변수로 구분
  return "production";
};

export const API_CONFIG: ApiConfig = {
  baseURL: API_URLS[getCurrentEnvironment()],
  timeout: {
    default: 10000, // 10초
    upload: 30000, // 파일 업로드는 30초
    download: 60000, // 파일 다운로드는 60초
  },
};

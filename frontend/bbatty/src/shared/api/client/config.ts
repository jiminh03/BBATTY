import { Platform } from "react-native";
//import { RuntimeEnvironment } from "../types/common";

interface TimeoutConfig {
  default: number;
  upload: number;
  download: number;
}

interface ErrorConfig {
  showToast: boolean;
  logToConsole: boolean;
  reportToService: boolean;
}

interface ApiConfig {
  baseURL: string;
  timeout: TimeoutConfig;
  errors: ErrorConfig;
}

/*
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
*/

export const API_CONFIG: ApiConfig = {
  baseURL: "--", //API_URLS[getCurrentEnvironment()],

  timeout: {
    default: 10000, // 10초
    upload: 30000, // 파일 업로드는 30초
    download: 60000, // 파일 다운로드는 60초
  },

  errors: {
    showToast: true, // 에러 토스트 표시 여부
    logToConsole: __DEV__, // 콘솔 로그 출력 여부
    reportToService: !__DEV__, // 에러 리포팅 서비스 전송 여부
  },
};

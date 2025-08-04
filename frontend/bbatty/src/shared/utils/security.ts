import * as SecureStore from 'expo-secure-store';
import CryptoJS from 'crypto-js';
import { Platform } from 'react-native';

// 보안 저장소 키
const SECURE_KEYS = {
  ENCRYPTION_KEY: 'app_encryption_key',
  DEVICE_ID: 'device_unique_id',
  BIOMETRIC_ENABLED: 'biometric_auth_enabled',
} as const;

// 암호화 클래스
export class Encryption {
  private static secretKey: string | null = null;

  // 암호화 키 초기화
  static async initialize(): Promise<void> {
    try {
      let key = await SecureStore.getItemAsync(SECURE_KEYS.ENCRYPTION_KEY);

      if (!key) {
        // 새로운 키 생성
        key = this.generateKey();
        await SecureStore.setItemAsync(SECURE_KEYS.ENCRYPTION_KEY, key);
      }

      this.secretKey = key;
    } catch (error) {
      console.error('암호화 키 초기화 실패:', error);
      // 폴백: 임시 키 사용
      this.secretKey = this.generateKey();
    }
  }

  // 랜덤 키 생성
  private static generateKey(): string {
    const array = new Uint8Array(32);
    if (typeof window !== 'undefined' && window.crypto) {
      window.crypto.getRandomValues(array);
    } else {
      // React Native 환경
      for (let i = 0; i < array.length; i++) {
        array[i] = Math.floor(Math.random() * 256);
      }
    }
    return Array.from(array, (byte) => byte.toString(16).padStart(2, '0')).join('');
  }

  // 데이터 암호화
  static encrypt(data: string): string {
    if (!this.secretKey) {
      throw new Error('암호화 키가 초기화되지 않았습니다');
    }

    try {
      return CryptoJS.AES.encrypt(data, this.secretKey).toString();
    } catch (error) {
      console.error('암호화 실패:', error);
      throw error;
    }
  }

  // 데이터 복호화
  static decrypt(encryptedData: string): string {
    if (!this.secretKey) {
      throw new Error('암호화 키가 초기화되지 않았습니다');
    }

    try {
      const bytes = CryptoJS.AES.decrypt(encryptedData, this.secretKey);
      return bytes.toString(CryptoJS.enc.Utf8);
    } catch (error) {
      console.error('복호화 실패:', error);
      throw error;
    }
  }

  // 해시 생성
  static hash(data: string): string {
    return CryptoJS.SHA256(data).toString();
  }

  // HMAC 생성
  static hmac(data: string, key: string): string {
    return CryptoJS.HmacSHA256(data, key).toString();
  }
}

// 민감한 데이터 마스킹
export const maskSensitiveData = (data: string, type: 'email' | 'phone' | 'name' | 'custom'): string => {
  switch (type) {
    case 'email': {
      const [username, domain] = data.split('@');
      if (!username || !domain) return data;

      const maskedUsername =
        username.length > 3 ? username.substring(0, 3) + '*'.repeat(username.length - 3) : '*'.repeat(username.length);

      return `${maskedUsername}@${domain}`;
    }

    case 'phone': {
      const cleaned = data.replace(/\D/g, '');
      if (cleaned.length < 10) return data;

      return cleaned.replace(/(\d{3})(\d{3,4})(\d{4})/, '$1-****-$3');
    }

    case 'name': {
      if (data.length <= 1) return data;
      return data[0] + '*'.repeat(data.length - 1);
    }

    case 'custom':
    default: {
      if (data.length <= 4) return '*'.repeat(data.length);
      return data.substring(0, 2) + '*'.repeat(data.length - 4) + data.substring(data.length - 2);
    }
  }
};

// 딥링크/URL 검증 - React Native에서 중요!
export const validateDeepLink = (url: string): boolean => {
  try {
    const parsed = new URL(url);

    // 허용된 스킴만 허용 (앱의 커스텀 스킴 포함)
    const allowedSchemes = ['https:', 'http:', 'baseballmatching:'];
    if (!allowedSchemes.includes(parsed.protocol)) {
      return false;
    }

    // 위험한 파라미터 체크
    const dangerousParams = ['javascript:', 'data:', 'vbscript:'];
    const urlString = url.toLowerCase();

    return !dangerousParams.some((param) => urlString.includes(param));
  } catch {
    return false;
  }
};

// API 요청 파라미터 검증
export const sanitizeApiParam = (param: string): string => {
  // 특수문자 제거 (API 서버 보호)
  return param
    .replace(/[<>'"]/g, '') // 기본적인 위험 문자 제거
    .trim()
    .substring(0, 100); // 길이 제한
};

// 사용자 입력 검증 (채팅, 댓글 등)
export const sanitizeUserContent = (content: string): string => {
  return content
    .replace(/[<>]/g, '') // HTML 태그 방지
    .replace(/javascript:/gi, '') // 스크립트 실행 방지
    .replace(/on\w+\s*=/gi, '') // 이벤트 핸들러 방지
    .trim();
};

// 안전한 URL 생성
export const sanitizeUrl = (url: string): string | null => {
  try {
    const parsed = new URL(url);

    // 허용된 프로토콜만 허용
    const allowedProtocols = ['http:', 'https:'];
    if (!allowedProtocols.includes(parsed.protocol)) {
      return null;
    }

    // JavaScript 실행 방지
    if (parsed.href.toLowerCase().includes('javascript:')) {
      return null;
    }

    return parsed.href;
  } catch {
    return null;
  }
};

// 디바이스 ID 관리
export class DeviceIdentifier {
  private static deviceId: string | null = null;

  static async getDeviceId(): Promise<string> {
    if (this.deviceId) {
      return this.deviceId;
    }

    try {
      // 저장된 디바이스 ID 확인
      let id = await SecureStore.getItemAsync(SECURE_KEYS.DEVICE_ID);

      if (!id) {
        // 새로운 디바이스 ID 생성
        id = this.generateDeviceId();
        await SecureStore.setItemAsync(SECURE_KEYS.DEVICE_ID, id);
      }

      this.deviceId = id;
      return id;
    } catch (error) {
      console.error('디바이스 ID 생성 실패:', error);
      // 메모리에만 저장되는 임시 ID 반환
      this.deviceId = this.generateDeviceId();
      return this.deviceId;
    }
  }

  private static generateDeviceId(): string {
    const timestamp = Date.now().toString(36);
    const randomPart = Math.random().toString(36).substring(2, 15);
    const platform = Platform.OS.substring(0, 3);

    return `${platform}_${timestamp}_${randomPart}`;
  }
}

// 세션 타임아웃 관리
export class SessionManager {
  private static timeoutId: NodeJS.Timeout | null = null;
  private static lastActivity: number = Date.now();
  private static readonly TIMEOUT_DURATION = 30 * 60 * 1000; // 30분

  static startSession(onTimeout: () => void): void {
    this.updateActivity();

    // 기존 타이머 정리
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }

    // 새 타이머 설정
    this.timeoutId = setInterval(() => {
      const inactiveTime = Date.now() - this.lastActivity;

      if (inactiveTime >= this.TIMEOUT_DURATION) {
        this.endSession();
        onTimeout();
      }
    }, 60000); // 1분마다 체크
  }

  static updateActivity(): void {
    this.lastActivity = Date.now();
  }

  static endSession(): void {
    if (this.timeoutId) {
      clearInterval(this.timeoutId);
      this.timeoutId = null;
    }
  }

  static getInactiveTime(): number {
    return Date.now() - this.lastActivity;
  }
}

// 안전한 JSON 파싱
export const safeJsonParse = <T>(json: string, fallback: T): T => {
  try {
    return JSON.parse(json);
  } catch {
    return fallback;
  }
};

// 입력 검증 헬퍼
export const sanitizeInput = (
  input: string,
  options?: {
    maxLength?: number;
    allowedChars?: RegExp;
    trim?: boolean;
  }
): string => {
  let sanitized = input;

  const { maxLength = 1000, allowedChars, trim = true } = options || {};

  // 트림
  if (trim) {
    sanitized = sanitized.trim();
  }

  // 길이 제한
  if (sanitized.length > maxLength) {
    sanitized = sanitized.substring(0, maxLength);
  }

  // 허용된 문자만 남기기
  if (allowedChars) {
    sanitized = sanitized.replace(new RegExp(`[^${allowedChars.source}]`, 'g'), '');
  }

  return sanitized;
};

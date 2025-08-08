import * as SecureStore from 'expo-secure-store';
import { STORAGE_KEYS } from '../../constants/storageKeys';

interface TokenManager {
  setToken(token: string): Promise<void>;
  getToken(): Promise<string | null>;
  removeToken(): Promise<void>;
  setRefreshToken(token: string): Promise<void>;
  getRefreshToken(): Promise<string | null>;
  removeRefreshToken(): Promise<void>;
  clearAllTokens(): Promise<void>;
}

export const tokenManager: TokenManager = {
  async setToken(token: string): Promise<void> {
    try {
      await SecureStore.setItemAsync(STORAGE_KEYS.AUTH_TOKEN, token);
    } catch (error) {
      throw new Error('토큰 저장 실패');
    }
  },

  async getToken(): Promise<string | null> {
    try {
      // 디바이스에 저장됐던 토큰 => 메모리로 복원
      return await SecureStore.getItemAsync(STORAGE_KEYS.AUTH_TOKEN);
    } catch (error) {
      return null;
    }
  },

  async removeToken(): Promise<void> {
    try {
      await SecureStore.deleteItemAsync(STORAGE_KEYS.AUTH_TOKEN);
    } catch (error) {
      console.warn('토큰 제거 실패', error);
    }
  },

  async setRefreshToken(token: string): Promise<void> {
    try {
      await SecureStore.setItemAsync(STORAGE_KEYS.REFRESH_TOKEN, token);
    } catch (error) {
      throw new Error('리프레시 토큰 저장 실패');
    }
  },

  async getRefreshToken(): Promise<string | null> {
    try {
      return await SecureStore.getItemAsync(STORAGE_KEYS.REFRESH_TOKEN);
    } catch (error) {
      return null;
    }
  },

  async removeRefreshToken(): Promise<void> {
    try {
      await SecureStore.deleteItemAsync(STORAGE_KEYS.REFRESH_TOKEN);
    } catch (error) {
      console.warn('리프레시 토큰 제거 실패', error);
    }
  },

  async clearAllTokens(): Promise<void> {
    try {
      await Promise.all([
        SecureStore.deleteItemAsync(STORAGE_KEYS.AUTH_TOKEN),
        SecureStore.deleteItemAsync(STORAGE_KEYS.REFRESH_TOKEN),
      ]);
    } catch (error) {
      console.warn('모든 토큰 제거 실패', error);
    }
  },
};

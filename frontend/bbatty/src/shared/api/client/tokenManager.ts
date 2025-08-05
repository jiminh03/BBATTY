import * as SecureStore from 'expo-secure-store';

const AUTH_TOKEN = 'authToken' as const;

interface TokenManager {
  setToken(token: string): Promise<void>;
  getToken(): Promise<string | null>;
  removeToken(): Promise<void>;
}

export const tokenManager: TokenManager = {
  async setToken(token: string): Promise<void> {
    try {
      await SecureStore.setItemAsync(AUTH_TOKEN, token);
    } catch (error) {
      console.warn('토큰을 저장하는데 실패하였습니다 : ', error);
      //throw하지 않으면 호출자가 실패를 모를 수 있어서 예상치 못한 버그가 발생 => 반환타입이 void라 별도 체크해줘야함
      throw new Error('토큰 저장 실패');
    }
  },

  async getToken(): Promise<string | null> {
    try {
      // 디바이스에 저장됐던 토큰 => 메모리로 복원
      return await SecureStore.getItemAsync(AUTH_TOKEN);
    } catch (error) {
      console.warn('토큰을 불러오는데 실패하였습니다 : ', error);
      return null;
    }
  },

  async removeToken(): Promise<void> {
    try {
      await SecureStore.deleteItemAsync(AUTH_TOKEN);
    } catch (error) {
      console.warn('토큰 제거 실패', error);
    }
  },
};

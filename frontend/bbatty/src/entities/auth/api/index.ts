import { apiClient, tokenManager, updateClientsToken } from '../../../shared';
/*

interface LoginRequest {
  email: string;
  password: string;
}

interface LoginResponse {
  token: string;
  user: {
    id: string;
    email: string;
    teamId: string;
  };
}

export const authApi = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const { data } = await apiClient.post<LoginResponse>('/auth/login', credentials);

    if (data.success && data.data) {
      // 토큰 저장
      await tokenManager.setToken(data.data.token);
      // 클라이언트 헤더 업데이트
      await updateClientsToken(data.data.token);
    }

    return data.data!;
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout');
    } finally {
      // 토큰 제거
      await tokenManager.removeToken();
      // 클라이언트 헤더 업데이트
      await updateClientsToken(null);
    }
  },

  async refreshToken(): Promise<string> {
    const { data } = await apiClient.post<{ token: string }>('/auth/refresh');
    
    if (data.success && data.data) {
      await tokenManager.setToken(data.data.token);
      await updateClientsToken(data.data.token);
      return data.data.token;
    }
    
    throw new Error('토큰 갱신 실패');
  }
};

// App.tsx (앱 초기화 예시)
import React, { useEffect, useState } from 'react';
import { initializeApiClient } from '@/shared/api';

export default function App() {
  const [isInitialized, setIsInitialized] = useState(false);

  useEffect(() => {
    const initApp = async () => {
      try {
        await initializeApiClient();
        setIsInitialized(true);
      } catch (error) {
        console.error('앱 초기화 실패:', error);
      }
    };

    initApp();
  }, []);

  if (!isInitialized) {
    return <SplashScreen />;
  }

  return <NavigationContainer>{ 앱 내용 }</NavigationContainer>;
}
*/

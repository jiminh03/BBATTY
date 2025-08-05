import { createStore, createAsyncAction } from '../../../shared';
import { tokenManager, updateClientsToken, extractData } from '../../../shared';
import { authApi } from '../api/authApi';
import type { AuthStore, RegisterRequest, KakaoLoginRequest } from './types';
import type { User } from '../../user/model/types';

const initialState = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
};

export const useAuthStore = createStore<AuthStore>(
  (set, get) => ({
    ...initialState,

    // 동기 액션들
    setUser: (user) => set({ user, isAuthenticated: !!user }),
    setLoading: (isLoading) => set({ isLoading }),
    setError: (error) => set({ error }),

    updateUser: (updates) =>
      set((state: any) => ({
        user: state.user ? { ...state.user, ...updates } : null,
      })),

    clearAuth: () => {
      tokenManager.removeToken();
      updateClientsToken(null);
      set(initialState);
    },

    // 비동기 액션들
    kakaoLogin: createAsyncAction(
      async (kakaoAccessToken: string) => {
        const apiResponse = (await authApi.kakaoLogin(kakaoAccessToken)).data;

        if (!apiResponse.success) throw new Error(`${apiResponse.error.code} ${apiResponse.error.details}`);

        const { user } = apiResponse.data;
        const { accessToken } = apiResponse.data.token;

        // 토큰 저장 및 클라이언트 업데이트
        await tokenManager.setToken(accessToken);
        await updateClientsToken(accessToken);

        set({ user, isAuthenticated: true, error: null });

        return apiResponse.data;
      },
      {
        onStart: () => set({ isLoading: true, error: null }),
        onError: (error) => set({ error: error.message, isLoading: false }),
        onFinally: () => set({ isLoading: false }),
      }
    ),

    logout: createAsyncAction(
      async () => {
        try {
          await authApi.logout();
        } catch (error) {
          // 로그아웃 API 실패해도 로컬 정리는 진행
          console.warn('Logout API failed:', error);
        } finally {
          get().clearAuth();
        }
      },
      {
        onStart: () => set({ isLoading: true }),
        onFinally: () => set({ isLoading: false }),
      }
    ),

    register: createAsyncAction(
      async (data: RegisterRequest) => {
        const apiResponse = (await authApi.register(data)).data;

        if (!apiResponse.success) throw new Error(`${apiResponse.error.code} ${apiResponse.error.details}`);

        const { user } = apiResponse.data;
        const { accessToken } = apiResponse.data.token;

        // 토큰 저장 및 클라이언트 업데이트
        await tokenManager.setToken(accessToken);
        await updateClientsToken(accessToken);

        set({ user, isAuthenticated: true, error: null });
      },
      {
        onStart: () => set({ isLoading: true, error: null }),
        onError: (error) => set({ error: error.message, isLoading: false }),
        onFinally: () => set({ isLoading: false }),
      }
    ),

    // refreshAuth: createAsyncAction(
    //   async () => {
    //     const token = await tokenManager.getToken();
    //     if (!token) {
    //       get().clearAuth();
    //       return;
    //     }

    //     try {
    //       // 토큰이 있으면 사용자 정보 조회
    //       const response = await authApi.fetchCurrentUser();
    //       const user = response.data.data;
    //       set({ user, isAuthenticated: true, error: null });
    //     } catch (error) {
    //       // 토큰 만료 등의 이유로 실패하면 로그아웃 처리
    //       get().clearAuth();
    //     }
    //   },
    //   {
    //     onStart: () => set({ isLoading: true }),
    //     onFinally: () => set({ isLoading: false }),
    //   }
    // ),
  }),
  {
    name: 'auth-store',
    enableDevtools: __DEV__,
    persist: {
      enabled: true,
      partialize: (state) => ({ user: state.user }),
      version: 1,
    },
  }
);

// 선택자 훅들
export const useUser = () => useAuthStore((state) => state.user);
export const useIsAuthenticated = () => useAuthStore((state) => state.isAuthenticated);
export const useAuthLoading = () => useAuthStore((state) => state.isLoading);
export const useAuthError = () => useAuthStore((state) => state.error);

// 액션 훅들
export const useAuthActions = () => {
  const { kakaoLogin, logout, register, updateUser, clearAuth /*refreshAuth*/ } = useAuthStore();
  return { kakaoLogin, logout, register, updateUser, clearAuth /*refreshAuth*/ };
};

// shared/store/tokenStore.ts
import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';
import { wrapAsync, Result, AsyncResult, isErr, isOk, Err, Ok } from '../../utils/result';
import { STORAGE_KEYS } from '../../constants/storageKeys';
import { wrapApiCall } from '../utils/apiWrapper';
import { Token } from './tokenTypes';
import { AxiosInstance } from 'axios';

export interface TokenError {
  type: 'STORAGE_ERROR' | 'NOT_FOUND' | 'REFRESH_FAILED';
  message: string;
  originalError?: unknown;
}

const createTokenError = (type: TokenError['type'], message: string, originalError?: unknown): TokenError => ({
  type,
  message,
  originalError,
});

interface TokenState {
  accessToken: string | null;
  refreshToken: string | null;
  accessTokenExpiresAt: string | null;
  refreshTokenExpiresAt: string | null;
  isTokenInitialized: boolean;
  isRefreshing: boolean;
  apiClient: AxiosInstance | null;
  refreshPromise: Promise<Result<boolean, TokenError>> | null;
  lastRefreshTime: number | null;
  proactiveRefreshInterval: NodeJS.Timeout | null;
  lastExpiryCheckTime: number | null;
  lastExpiryCheckResult: boolean | null;
}

interface TokenActions {
  setApiClient: (client: AxiosInstance) => void;
  initializeTokens: () => Promise<Result<void, TokenError>>;
  setTokens: (tokens: Token) => Promise<Result<void, TokenError>>;
  clearTokens: () => Promise<Result<void, TokenError>>;
  refreshTokens: () => Promise<Result<boolean, TokenError>>;
  checkAndRefreshIfNeeded: () => Promise<Result<boolean, TokenError>>;
  startProactiveRefresh: () => void;
  getAccessToken: () => string | null;
  hasValidTokens: () => boolean;
  hasRefreshToken: () => boolean;
  isAccessTokenExpiringSoon: (minutesBeforeExpiry?: number) => boolean;
  getCurrentTokens: () => Token | null;
  isRefreshTokenExpired: () => boolean;
  resetToken: () => void;
}

type TokenStore = TokenState & TokenActions;

const DEFAULT_REFRESH_THRESHOLD_MINUTES = 10;

export const useTokenStore = create<TokenStore>((set, get) => ({
  accessToken: null,
  refreshToken: null,
  accessTokenExpiresAt: null,
  refreshTokenExpiresAt: null,
  isTokenInitialized: false,
  isRefreshing: false,
  apiClient: null,
  refreshPromise: null,
  lastRefreshTime: null,
  proactiveRefreshInterval: null,
  lastExpiryCheckTime: null,
  lastExpiryCheckResult: null,

  setApiClient: (client: AxiosInstance) => {
    set({ apiClient: client });
  },

  initializeTokens: async (): Promise<Result<void, TokenError>> => {
    return wrapAsync(
      async () => {
        const [access, refresh, accessExpiry, refreshExpiry] = await Promise.all([
          SecureStore.getItemAsync(STORAGE_KEYS.ACCESS_TOKEN),
          SecureStore.getItemAsync(STORAGE_KEYS.REFRESH_TOKEN),
          SecureStore.getItemAsync(STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT),
          SecureStore.getItemAsync(STORAGE_KEYS.REFRESH_TOKEN_EXPIRES_AT),
        ]);

        set({
          accessToken: access,
          refreshToken: refresh,
          accessTokenExpiresAt: accessExpiry,
          refreshTokenExpiresAt: refreshExpiry,
          isTokenInitialized: true,
        });
      },
      (error) => {
        set({ isTokenInitialized: false });
        return createTokenError('STORAGE_ERROR', 'Failed to initialize tokens', error);
      }
    );
  },

  setTokens: async (tokens: Token): Promise<Result<void, TokenError>> => {
    return wrapAsync(
      async () => {
        const storePromises = [
          SecureStore.setItemAsync(STORAGE_KEYS.ACCESS_TOKEN, tokens.accessToken),
          SecureStore.setItemAsync(STORAGE_KEYS.REFRESH_TOKEN, tokens.refreshToken),
          SecureStore.setItemAsync(STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT, tokens.accessTokenExpiresAt),
          SecureStore.setItemAsync(STORAGE_KEYS.REFRESH_TOKEN_EXPIRES_AT, tokens.refreshTokenExpiresAt),
        ];

        await Promise.all(storePromises);

        set({
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          accessTokenExpiresAt: tokens.accessTokenExpiresAt,
          refreshTokenExpiresAt: tokens.refreshTokenExpiresAt,
        });


        // 토큰 저장 후 선제적 갱신 시작
        get().startProactiveRefresh();
      },
      (error) => createTokenError('STORAGE_ERROR', 'Failed to set tokens', error)
    );
  },

  clearTokens: async (): Promise<Result<void, TokenError>> => {
    return wrapAsync(
      async () => {
        await Promise.all([
          SecureStore.deleteItemAsync(STORAGE_KEYS.ACCESS_TOKEN),
          SecureStore.deleteItemAsync(STORAGE_KEYS.REFRESH_TOKEN),
          SecureStore.deleteItemAsync(STORAGE_KEYS.ACCESS_TOKEN_EXPIRES_AT),
          SecureStore.deleteItemAsync(STORAGE_KEYS.REFRESH_TOKEN_EXPIRES_AT),
        ]);

        const { proactiveRefreshInterval } = get();
        if (proactiveRefreshInterval) {
          clearInterval(proactiveRefreshInterval);
        }

        set({
          accessToken: null,
          refreshToken: null,
          accessTokenExpiresAt: null,
          refreshTokenExpiresAt: null,
          isRefreshing: false,
          refreshPromise: null,
          lastRefreshTime: null,
          proactiveRefreshInterval: null,
          lastExpiryCheckTime: null,
          lastExpiryCheckResult: null,
        });
      },
      (error) => createTokenError('STORAGE_ERROR', 'Failed to clear tokens', error)
    );
  },

  isAccessTokenExpiringSoon: (minutesBeforeExpiry = DEFAULT_REFRESH_THRESHOLD_MINUTES): boolean => {
    const now = Date.now();
    const { lastExpiryCheckTime, lastExpiryCheckResult, accessTokenExpiresAt } = get();

    // 1초 이내 중복 체크 방지 (캐싱)
    if (lastExpiryCheckTime && now - lastExpiryCheckTime < 1000 && lastExpiryCheckResult !== null) {
      return lastExpiryCheckResult;
    }

    if (!accessTokenExpiresAt) {
      set({ lastExpiryCheckTime: now, lastExpiryCheckResult: false });
      return false;
    }

    try {
      const expiryDate = new Date(accessTokenExpiresAt);
      const nowDate = new Date();
      const thresholdTime = new Date(expiryDate.getTime() - minutesBeforeExpiry * 60 * 1000);

      const isExpiring = nowDate >= thresholdTime;

      // 캐시 업데이트
      set({ lastExpiryCheckTime: now, lastExpiryCheckResult: isExpiring });


      return isExpiring;
    } catch (error) {
      set({ lastExpiryCheckTime: now, lastExpiryCheckResult: false });
      return false;
    }
  },

  checkAndRefreshIfNeeded: async (): Promise<Result<boolean, TokenError>> => {
    const { isAccessTokenExpiringSoon, refreshTokens, refreshPromise, lastRefreshTime } = get();

    // 이미 갱신 중인 Promise가 있다면 재사용
    if (refreshPromise) {
      return refreshPromise;
    }

    // 최근 30초 이내에 갱신했다면 스킵 (무한 갱신 방지)
    const now = Date.now();
    if (lastRefreshTime && now - lastRefreshTime < 30000) {
      return Ok(true);
    }

    // 토큰이 만료 임박하지 않으면 갱신하지 않음
    const isExpiringSoon = isAccessTokenExpiringSoon();

    if (!isExpiringSoon) {
      return Ok(true);
    }

    return await refreshTokens();
  },

  startProactiveRefresh: () => {
    const { proactiveRefreshInterval } = get();

    // 이미 실행 중이면 중단
    if (proactiveRefreshInterval) {
      clearInterval(proactiveRefreshInterval);
    }

    // 5분마다 토큰 만료 임박 여부 체크
    const interval = setInterval(async () => {
      const { isAccessTokenExpiringSoon, refreshTokens, isRefreshing, refreshPromise } = get();

      // 이미 갱신 중이면 스킵
      if (isRefreshing || refreshPromise) {
        return;
      }

      // 토큰 만료 임박 시 갱신
      if (isAccessTokenExpiringSoon()) {
        try {
          await refreshTokens();
        } catch (error) {
          // Silent fail for background refresh
        }
      }
    }, 5 * 60 * 1000); // 5분마다

    set({ proactiveRefreshInterval: interval });
  },

  refreshTokens: async (): Promise<Result<boolean, TokenError>> => {
    const state = get();

    // 이미 갱신 중인 Promise가 있다면 재사용
    if (state.refreshPromise) {
      return state.refreshPromise;
    }

    // 새로운 갱신 Promise 생성
    const refreshPromise = (async (): Promise<Result<boolean, TokenError>> => {
      const { refreshToken, apiClient } = get();

      if (!refreshToken) {
        return Err(createTokenError('NOT_FOUND', 'No refresh token available'));
      }

      if (!apiClient) {
        return Err(createTokenError('REFRESH_FAILED', 'API client not initialized'));
      }

      set({ isRefreshing: true });

      try {
        const result = await wrapApiCall<Token>(() => {
          return apiClient.post('/api/auth/refresh', null, {
            headers: { 'X-Refresh-Token': refreshToken } as any,
          });
        });

        if (isOk(result)) {
          const setResult = await get().setTokens(result.data);

          if (isErr(setResult)) {
            return Err(createTokenError('STORAGE_ERROR', 'Failed to save new tokens'));
          }

          // 갱신 완료 시간 기록
          set({ lastRefreshTime: Date.now() });

          return Ok(true);
        } else {
          await get().clearTokens();
          return Err(createTokenError('REFRESH_FAILED', result.error.message));
        }
      } catch (error) {
        await get().clearTokens();
        return Err(createTokenError('REFRESH_FAILED', 'Unexpected error during token refresh'));
      } finally {
        set({ isRefreshing: false, refreshPromise: null });
      }
    })();

    // Promise를 스토어에 저장
    set({ refreshPromise });

    return refreshPromise;
  },

  getAccessToken: () => get().accessToken,

  hasValidTokens: () => {
    const { accessToken, refreshToken } = get();
    return accessToken !== null && refreshToken !== null;
  },

  hasRefreshToken: () => {
    const { refreshToken } = get();
    return refreshToken !== null;
  },

  // Token 타입 완전 활용을 위한 추가 메서드들
  getCurrentTokens: (): Token | null => {
    const { accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt } = get();

    if (!accessToken || !refreshToken) {
      return null;
    }

    return {
      accessToken,
      refreshToken,
      accessTokenExpiresAt: accessTokenExpiresAt || '',
      refreshTokenExpiresAt: refreshTokenExpiresAt || '',
    };
  },

  isRefreshTokenExpired: (): boolean => {
    const { refreshTokenExpiresAt } = get();
    if (!refreshTokenExpiresAt) return true; // 만료시간 정보가 없으면 유효하지 않다고 판단

    try {
      const expiryDate = new Date(refreshTokenExpiresAt);
      const now = new Date();
      return now >= expiryDate;
    } catch (error) {
      console.error('Failed to parse refresh token expiry date:', error);
      return false;
    }
  },

  resetToken: () => {

    const { proactiveRefreshInterval } = get();
    if (proactiveRefreshInterval) {
      clearInterval(proactiveRefreshInterval);
    }

    set({
      accessToken: null,
      refreshToken: null,
      accessTokenExpiresAt: null,
      refreshTokenExpiresAt: null,
      isRefreshing: false,
      refreshPromise: null,
      lastRefreshTime: null,
      proactiveRefreshInterval: null,
      lastExpiryCheckTime: null,
      lastExpiryCheckResult: null,
    });
  },
}));

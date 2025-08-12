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
}

interface TokenActions {
  setApiClient: (client: AxiosInstance) => void;
  initializeTokens: () => Promise<Result<void, TokenError>>;
  setTokens: (tokens: Token) => Promise<Result<void, TokenError>>;
  clearTokens: () => Promise<Result<void, TokenError>>;
  refreshTokens: () => Promise<Result<boolean, TokenError>>;
  checkAndRefreshIfNeeded: () => Promise<Result<boolean, TokenError>>;
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
          isTokenInitialized: false,
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

        console.log('🔑 [TokenStore] Access Token 저장 성공:', {
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          accessTokenExpiresAt: tokens.accessTokenExpiresAt,
          refreshTokenExpiresAt: tokens.refreshTokenExpiresAt,
        });
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

        set({
          accessToken: null,
          refreshToken: null,
          accessTokenExpiresAt: null,
          refreshTokenExpiresAt: null,
          isRefreshing: false,
        });
      },
      (error) => createTokenError('STORAGE_ERROR', 'Failed to clear tokens', error)
    );
  },

  isAccessTokenExpiringSoon: (minutesBeforeExpiry = DEFAULT_REFRESH_THRESHOLD_MINUTES): boolean => {
    const { accessTokenExpiresAt } = get();
    if (!accessTokenExpiresAt) return false;

    try {
      const expiryDate = new Date(accessTokenExpiresAt);
      const now = new Date();
      const thresholdTime = new Date(expiryDate.getTime() - minutesBeforeExpiry * 60 * 1000);

      return now >= thresholdTime;
    } catch (error) {
      console.error('Failed to parse access token expiry date:', error);
      return false;
    }
  },

  checkAndRefreshIfNeeded: async (): Promise<Result<boolean, TokenError>> => {
    const { isAccessTokenExpiringSoon, refreshTokens, isRefreshing } = get();

    // 이미 갱신 중이면 대기
    if (isRefreshing) {
      return new Promise((resolve) => {
        const checkComplete = () => {
          if (!get().isRefreshing) {
            resolve(Ok(get().accessToken !== null));
          } else {
            setTimeout(checkComplete, 100);
          }
        };
        checkComplete();
      });
    }

    // 토큰이 만료 임박하지 않으면 갱신하지 않음
    if (!isAccessTokenExpiringSoon()) {
      return Ok(true);
    }

    console.log('10분전 미리 갱신 ');
    console.log('Access token expiring soon, attempting refresh...');
    return await refreshTokens();
  },

  refreshTokens: async (): Promise<Result<boolean, TokenError>> => {
    const { refreshToken, isRefreshing, apiClient } = get();

    if (isRefreshing) {
      return new Promise((resolve) => {
        const checkComplete = () => {
          if (!get().isRefreshing) {
            resolve(Ok(get().accessToken !== null));
          } else {
            setTimeout(checkComplete, 100);
          }
        };
        checkComplete();
      });
    }

    if (!refreshToken) {
      return Err(createTokenError('NOT_FOUND', 'No refresh token available'));
    }

    if (!apiClient) {
      return Err(createTokenError('REFRESH_FAILED', 'API client not initialized'));
    }

    set({ isRefreshing: true });

    const result = await wrapApiCall<Token>(() => {
      return apiClient.post('/api/auth/refresh', null, {
        headers: { 'X-Refresh-Token': refreshToken } as any,
      });
    });

    set({ isRefreshing: false });

    if (isOk(result)) {
      // Token 타입 완전 활용 - setTokens 직접 사용
      const setResult = await get().setTokens(result.data);

      if (isErr(setResult)) {
        return Err(createTokenError('STORAGE_ERROR', 'Failed to save new tokens'));
      }

      console.log('Tokens refreshed successfully');
      return Ok(true);
    } else {
      await get().clearTokens();
      return Err(createTokenError('REFRESH_FAILED', result.error.message));
    }
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

  resetToken: () =>
    set({
      accessToken: null,
      refreshToken: null,
    }),
}));

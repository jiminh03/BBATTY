// shared/store/tokenStore.ts
import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';
import { wrapAsync, Result, AsyncResult, isErr, isOk, Err, Ok } from '../../utils/result';
import { apiClient } from '..';
import { STORAGE_KEYS } from '../../constants/storageKeys';
import { wrapApiCall } from '../utils/apiWrapper';
import { Token } from './tokenTypes';

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
  isTokenInitialized: boolean;
  isRefreshing: boolean;
}

interface TokenActions {
  initializeTokens: () => Promise<Result<void, TokenError>>;
  setTokens: (access: string, refresh: string) => Promise<Result<void, TokenError>>;
  clearTokens: () => Promise<Result<void, TokenError>>;
  refreshTokens: () => Promise<Result<boolean, TokenError>>;
  getAccessToken: () => string | null;
  hasValidTokens: () => boolean;
}

type TokenStore = TokenState & TokenActions;

export const useTokenStore = create<TokenStore>((set, get) => ({
  accessToken: null,
  refreshToken: null,
  isTokenInitialized: false,
  isRefreshing: false,

  initializeTokens: async (): Promise<Result<void, TokenError>> => {
    return wrapAsync(
      async () => {
        const [access, refresh] = await Promise.all([
          SecureStore.getItemAsync(STORAGE_KEYS.ACCESS_TOKEN),
          SecureStore.getItemAsync(STORAGE_KEYS.REFRESH_TOKEN),
        ]);

        set({
          accessToken: access,
          refreshToken: refresh,
          isTokenInitialized: true,
        });
      },
      (error) => {
        set({ isTokenInitialized: false });
        return createTokenError('STORAGE_ERROR', 'Failed to initialize tokens', error);
      }
    );
  },

  setTokens: async (access: string, refresh: string): Promise<Result<void, TokenError>> => {
    return wrapAsync(
      async () => {
        await Promise.all([
          SecureStore.setItemAsync(STORAGE_KEYS.ACCESS_TOKEN, access),
          SecureStore.setItemAsync(STORAGE_KEYS.REFRESH_TOKEN, refresh),
        ]);

        set({ accessToken: access, refreshToken: refresh });
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
        ]);

        set({
          accessToken: null,
          refreshToken: null,
          isRefreshing: false,
        });
      },
      (error) => createTokenError('STORAGE_ERROR', 'Failed to clear tokens', error)
    );
  },

  refreshTokens: async (): Promise<Result<boolean, TokenError>> => {
    const { refreshToken, isRefreshing } = get();

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

    set({ isRefreshing: true });

    // authApi 사용으로 단순화
    const result = await wrapApiCall<Token>(() => {
      return apiClient.post('/api/auth/refresh', null, {
        headers: { 'X-Refresh-Token': refreshToken } as any,
      });
    });

    set({ isRefreshing: false });

    if (isOk(result)) {
      const { accessToken: newAccess, refreshToken: newRefresh } = result.data;
      const setResult = await get().setTokens(newAccess, newRefresh);

      if (isErr(setResult)) {
        return Err(createTokenError('STORAGE_ERROR', 'Failed to save new tokens'));
      }

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
}));

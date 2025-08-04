import { create } from 'zustand';
import { devtools, persist, subscribeWithSelector } from 'zustand/middleware';
import { immer } from 'zustand/middleware/immer';
import { createAsyncStoragePersist } from './middleware';

interface CreateStoreOptions {
  name: string;
  enableDevtools?: boolean;
  enableImmer?: boolean;
  persist?: {
    enabled: boolean;
    partialize?: (state: any) => any;
    version?: number;
  };
}

// 간단한 store 생성
export const createStore = <T extends object>(
  storeCreator: (set: any, get: any, api: any) => T,
  options: CreateStoreOptions
) => {
  const { name, enableDevtools = __DEV__, enableImmer = true, persist: persistOptions } = options;

  // Immer 사용
  if (enableImmer) {
    if (persistOptions?.enabled) {
      return create<T>()(
        devtools(
          persist(subscribeWithSelector(immer<T>((set, get, api) => storeCreator(set, get, api))), {
            ...createAsyncStoragePersist(name),
            partialize: persistOptions.partialize,
            version: persistOptions.version || 1,
          }),
          enableDevtools ? { name } : undefined
        )
      );
    }

    return create<T>()(
      devtools(
        subscribeWithSelector(immer<T>((set, get, api) => storeCreator(set, get, api))),
        enableDevtools ? { name } : undefined
      )
    );
  }

  // Immer 미사용
  if (persistOptions?.enabled) {
    return create<T>()(
      devtools(
        persist(subscribeWithSelector(storeCreator), {
          ...createAsyncStoragePersist(name),
          partialize: persistOptions.partialize,
          version: persistOptions.version || 1,
        }),
        enableDevtools ? { name } : undefined
      )
    );
  }

  return create<T>()(devtools(subscribeWithSelector(storeCreator), enableDevtools ? { name } : undefined));
};

// 슬라이스 패턴을 위한 헬퍼
export const createSlice =
  <T extends object>(initialState: T, actions: (set: any, get: any) => any) =>
  (set: any, get: any) => ({
    ...initialState,
    ...actions(set, get),
  });

// 비동기 액션 생성 헬퍼
export const createAsyncAction = <TArgs extends any[], TResult>(
  action: (...args: TArgs) => Promise<TResult>,
  options?: {
    onStart?: () => void;
    onSuccess?: (result: TResult) => void;
    onError?: (error: Error) => void;
    onFinally?: () => void;
  }
) => {
  return async (...args: TArgs): Promise<TResult | null> => {
    try {
      options?.onStart?.();
      const result = await action(...args);
      options?.onSuccess?.(result);
      return result;
    } catch (error) {
      options?.onError?.(error as Error);
      return null;
    } finally {
      options?.onFinally?.();
    }
  };
};

// Store 리셋 헬퍼
export const createResetAction = <T extends object>(initialState: T) => {
  return (set: any) => ({
    reset: () => set(initialState),
  });
};

import { StateCreator, StoreMutatorIdentifier } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';

// 로깅 미들웨어
export const logger =
  <T extends object>(config: StateCreator<T, [], []>): StateCreator<T, [], []> =>
  (set, get, api) =>
    config(
      (args) => {
        if (__DEV__) {
          console.log('  applying', args);
          set(args);
          console.log('  new state', get());
        } else {
          set(args);
        }
      },
      get,
      api
    );

// 액션 이름 로깅 미들웨어
export const actionLogger =
  <T extends object>(config: StateCreator<T, [], []>): StateCreator<T, [], []> =>
  (set, get, api) => {
    const loggedSet: typeof set = (args) => {
      if (__DEV__) {
        if (typeof args === 'function') {
          console.log('🔄 Store Action Called');
        } else {
          console.log('🔄 Store State Updated:', args);
        }
      }
      set(args);
    };

    return config(loggedSet, get, api);
  };

// 에러 바운더리 미들웨어
export const errorHandler =
  <T extends object>(config: StateCreator<T, [], []>): StateCreator<T, [], []> =>
  (set, get, api) =>
    config(
      (args) => {
        try {
          set(args);
        } catch (error) {
          console.error(' Store Error:', error);
        }
      },
      get,
      api
    );

// 성능 모니터링 미들웨어
export const performanceMonitor =
  <T extends object>(config: StateCreator<T, [], []>): StateCreator<T, [], []> =>
  (set, get, api) => {
    const monitoredSet: typeof set = (args) => {
      if (__DEV__) {
        const startTime = performance.now();
        set(args);
        const endTime = performance.now();
        const duration = endTime - startTime;

        if (duration > 16) {
          // 60fps 기준 (1000ms / 60 ≈ 16.67ms)
          console.warn(`Slow state update: ${duration.toFixed(2)}ms`);
        }
      } else {
        set(args);
      }
    };

    return config(monitoredSet, get, api);
  };

// AsyncStorage persist 헬퍼
export const createAsyncStoragePersist = (name: string) => ({
  name,
  storage: {
    getItem: async (key: string) => {
      const value = await AsyncStorage.getItem(key);
      return value ? JSON.parse(value) : null;
    },
    setItem: async (key: string, value: any) => {
      await AsyncStorage.setItem(key, JSON.stringify(value));
    },
    removeItem: async (key: string) => {
      await AsyncStorage.removeItem(key);
    },
  },
});

// 선택적 persist를 위한 헬퍼
export const partializeState =
  <T extends object>(keysToInclude: (keyof T)[]) =>
  (state: T) => {
    const partialState: Partial<T> = {};
    keysToInclude.forEach((key) => {
      if (key in state) {
        partialState[key] = state[key];
      }
    });
    return partialState;
  };

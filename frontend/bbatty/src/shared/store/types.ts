import { StateCreator, StoreApi } from 'zustand';

// Store 슬라이스 패턴을 위한 타입
export type StateSlice<T> = StateCreator<T, [], [], T>;

// 비동기 액션 결과 타입
export interface AsyncActionResult<T = void> {
  success: boolean;
  data?: T;
  error?: string;
}

// Store 리셋 가능한 인터페이스
export interface Resettable {
  reset: () => void;
}

// 로딩 상태를 가진 Store 인터페이스
export interface WithLoading {
  isLoading: boolean;
  setLoading: (loading: boolean) => void;
}

// 에러 상태를 가진 Store 인터페이스
export interface WithError {
  error: string | null;
  setError: (error: string | null) => void;
  clearError: () => void;
}

// 공통 Store 유틸리티 타입
export type StoreActions<T> = {
  [K in keyof T]: T[K] extends (...args: any[]) => any ? K : never;
}[keyof T];

export type StoreState<T> = Omit<T, StoreActions<T>>;

// Persist 옵션 타입
export interface PersistOptions {
  name: string;
  version?: number;
  migrate?: (persistedState: any, version: number) => any;
  partialize?: (state: any) => any;
}

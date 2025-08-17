// entities/user/model/userStore.ts
import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { wrapAsync, Result, isOk } from '../../../shared/utils/result';
import { User } from './userTypes';
import { STORAGE_KEYS } from '../../../shared';

interface UserState {
  currentUser: User | null;
  isUserInitialized: boolean;
}

interface UserActions {
  initializeUser: () => Promise<Result<void, Error>>;
  hasUser: () => Promise<Result<boolean, Error>>;
  getCurrentUser: () => User | null;
  setCurrentUser: (user: User | null) => Promise<Result<void, Error>>;
  logout: () => Promise<Result<void, Error>>;
  reset: () => Promise<void>;
  withdraw: () => Promise<void>;
}

type UserStore = UserState & UserActions;

export const useUserStore = create<UserStore>((set, get) => ({
  currentUser: null,
  isUserInitialized: false,

  initializeUser: async () => {
    const result = await wrapAsync(async () => {
      const data = await AsyncStorage.getItem(STORAGE_KEYS.USER);
      const user = data ? JSON.parse(data) : null;
      set({ currentUser: user, isUserInitialized: true });
    });

    if (!isOk(result)) {
      console.error('User initialization failed:', result.error);
      set({ currentUser: null, isUserInitialized: true });
    }

    return result;
  },

  getCurrentUser: () => get().currentUser,

  setCurrentUser: async (user) => {
    const result = await wrapAsync(async () => {
      if (user) {
        await AsyncStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
      } else {
        await AsyncStorage.removeItem(STORAGE_KEYS.USER);
      }
      set({ currentUser: user });
    });

    if (!isOk(result)) {
      console.error('Failed to set user:', result.error);
    }

    return result;
  },

  hasUser: async () => {
    const result = await wrapAsync(async () => {
      const data = await AsyncStorage.getItem(STORAGE_KEYS.USER);

      console.log('hasUserResult :', data);
      return data !== null;
    });

    return result;
  },

  logout: async () => {
    const result = await wrapAsync(async () => {
      await AsyncStorage.removeItem(STORAGE_KEYS.USER);
      set({
        currentUser: null,
      });
    });

    if (!isOk(result)) {
      console.error('Logout failed:', result.error);
    }

    return result;
  },

  reset: async () => {
    set({
      currentUser: null,
    });
    await AsyncStorage.removeItem(STORAGE_KEYS.USER);
  },

  withdraw: async () => {
    // 사용자 관련 상태 초기화
    set({
      currentUser: null,
      isUserInitialized: false,
    });
    await AsyncStorage.removeItem(STORAGE_KEYS.USER);
    
    // 전역 상태에 탈퇴 플래그 설정하여 AppNavigator에 알림
    global.isWithdrawing = true;
  },
}));

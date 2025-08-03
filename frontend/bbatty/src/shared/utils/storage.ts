import AsyncStorage from '@react-native-async-storage/async-storage';
import { STORAGE_KEYS } from '../constants';

export const storage = {
  async get<T>(key: keyof typeof STORAGE_KEYS): Promise<T | null> {
    try {
      const value = await AsyncStorage.getItem(STORAGE_KEYS[key]);
      return value ? JSON.parse(value) : null;
    } catch {
      return null;
    }
  },

  async set<T>(key: keyof typeof STORAGE_KEYS, value: T): Promise<void> {
    await AsyncStorage.setItem(STORAGE_KEYS[key], JSON.stringify(value));
  },

  async remove(key: keyof typeof STORAGE_KEYS): Promise<void> {
    await AsyncStorage.removeItem(STORAGE_KEYS[key]);
  },

  async clear(): Promise<void> {
    await AsyncStorage.clear();
  },
};

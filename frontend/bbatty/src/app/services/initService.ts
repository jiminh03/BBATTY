import { Alert } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { STORAGE_KEYS, APP_INFO, checkNetwork } from '../../shared';

export class AppInitService {
  static async checkNetworkConnection(): Promise<boolean> {
    const isConnected = await checkNetwork();

    if (!isConnected) {
      Alert.alert('네트워크 오류', '인터넷 연결을 확인해주세요. 일부 기능이 제한될 수 있습니다.', [{ text: '확인' }]);
    }

    return isConnected;
  }

  static async clearOldCache(): Promise<void> {
    try {
      const cacheKeys = [STORAGE_KEYS.GAME_SCHEDULE_CACHE, STORAGE_KEYS.STADIUM_INFO_CACHE];

      for (const key of cacheKeys) {
        const cachedData = await AsyncStorage.getItem(key);
        if (cachedData) {
          const parsed = JSON.parse(cachedData);
          // 7일 이상 된 캐시 삭제
          if (parsed.timestamp && Date.now() - parsed.timestamp > 7 * 24 * 60 * 60 * 1000) {
            await AsyncStorage.removeItem(key);
          }
        }
      }
    } catch (error) {
      console.error('캐시 정리 실패:', error);
    }
  }
}

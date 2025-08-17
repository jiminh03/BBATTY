import { Platform } from 'react-native';

// 네이티브 모듈 인터페이스
interface AppExitModule {
  finishAndRemoveTask(): void;
}

// 네이티브 모듈 가져오기 시도
let AppExit: AppExitModule | null = null;

try {
  if (Platform.OS === 'android') {
    // React Native CLI 환경에서 네이티브 모듈 사용
    const { NativeModules } = require('react-native');
    AppExit = NativeModules.AppExit;
  }
} catch (error) {
  console.log('Native AppExit module not available');
}

export const exitAppCompletely = async (): Promise<void> => {
  if (Platform.OS === 'android') {
    try {
      // 1. react-native-exit-app 사용 (최근 앱에서 제거)
      const RNExitApp = require('react-native-exit-app').default;
      RNExitApp.exitApp();
      return;
    } catch (error) {
      console.log('react-native-exit-app not available, trying native module');
    }

    try {
      // 2. 커스텀 네이티브 모듈 사용
      if (AppExit?.finishAndRemoveTask) {
        AppExit.finishAndRemoveTask();
        return;
      }
    } catch (error) {
      console.log('Custom native module not available');
    }

    // 3. Fallback to BackHandler
    try {
      const { BackHandler } = require('react-native');
      BackHandler.exitApp();
    } catch (error) {
      console.log('BackHandler.exitApp failed');
    }
  } else {
    // iOS는 앱 강제 종료를 허용하지 않음
    console.log('App exit not supported on iOS');
  }
};
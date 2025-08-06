import { createNavigationContainerRef } from '@react-navigation/native';
import { RootStackParamList } from './types';

// 네비게이션 참조 생성 - 컴포넌트 외부에서 네비게이션 사용하기 위함
export const navigationRef = createNavigationContainerRef<RootStackParamList>();

// 네비게이션 헬퍼 함수들
export function navigate(name: keyof RootStackParamList, params?: any) {
  if (navigationRef.isReady()) {
    navigationRef.navigate(name as any, params);
  }
}

export function goBack() {
  if (navigationRef.isReady() && navigationRef.canGoBack()) {
    navigationRef.goBack();
  }
}

export function reset(routes: any[]) {
  if (navigationRef.isReady()) {
    navigationRef.reset({
      index: 0,
      routes,
    });
  }
}

export function resetToAuth() {
  reset([{ name: 'AuthStack' }]);
}

export function resetToMain() {
  reset([{ name: 'MainTabs' }]);
}

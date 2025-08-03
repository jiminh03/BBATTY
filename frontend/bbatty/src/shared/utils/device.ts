import { Platform, Dimensions, Vibration } from 'react-native';
import NetInfo from '@react-native-community/netinfo';

// 플랫폼 정보
export const isIOS = Platform.OS === 'ios';
export const isAndroid = Platform.OS === 'android';

// 화면 정보
const { width: screenWidth, height: screenHeight } = Dimensions.get('window');

export const screen = {
  width: screenWidth,
  height: screenHeight,
  isSmall: screenWidth < 375,
  isTablet: screenWidth >= 768,
};

// Safe Area (노치 대응)
export const safeArea = {
  top: isIOS && screenHeight >= 812 ? 44 : 20,
  bottom: isIOS && screenHeight >= 812 ? 34 : 0,
};

// 네트워크 상태 확인
export const checkNetwork = async (): Promise<boolean> => {
  const state = await NetInfo.fetch();
  return state.isConnected || false;
};

// 간단한 햅틱 피드백
export const haptic = {
  light: () => Vibration.vibrate(10),
  medium: () => Vibration.vibrate(20),
  heavy: () => Vibration.vibrate(30),
};

// 화면 방향
export const getOrientation = () => {
  const { width, height } = Dimensions.get('window');
  return width > height ? 'landscape' : 'portrait';
};

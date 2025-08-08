import React, { useEffect, useState } from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { NavigationContainer } from '@react-navigation/native';
import { tokenManager } from '../shared';
import AuthNavigator from './AuthNavigator';
import MainNavigator from './MainNavigator';
import { linking } from './linking';
import SplashScreen from '../pages/splash/ui/SplashScreen';
import { navigationRef } from './navigationRefs';
import { useAuthStore } from '../entities/auth/model/authStore';
const Stack = createStackNavigator();

export default function AppNavigator() {
  const [isLoading, setIsLoading] = useState(true);
  const [showSplash, setShowSplash] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const { setKakaoUserInfo, setKakaoAccessToken } = useAuthStore();

  useEffect(() => {
    checkAuthState();
  }, []);

  const checkAuthState = async () => {
    try {
      const token = await tokenManager.getToken();
      // 개발 중에는 항상 인증된 상태로 설정 (테스트 목적)
      setIsAuthenticated(true);
      // setIsAuthenticated(!!token);
    } catch (error) {
      console.error('Auth check error ', error);
      setIsAuthenticated(true); // 개발 중 우회
      // setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoginSuccess = (userInfo: any, accessToken: string) => {
    setIsAuthenticated(true);
    setShowSplash(false);
    setKakaoUserInfo(userInfo);
    setKakaoAccessToken(accessToken);
  };

  if (showSplash) {
    return (
      <SplashScreen
        onAnimationComplete={() => {
          setShowSplash(false);
        }}
        onLoginSuccess={handleLoginSuccess}
      ></SplashScreen>
    );
  }

  return (
    <NavigationContainer ref={navigationRef} linking={linking}>
      {/* <StatusBar barStyle='light-content' backgroundColor={theme.colors.background} /> */}
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name='AuthStack' component={AuthNavigator} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

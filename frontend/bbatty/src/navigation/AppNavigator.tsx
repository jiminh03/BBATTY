import React, { useEffect, useState } from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { NavigationContainer } from '@react-navigation/native';
import { tokenManager } from '../shared';
import { setUnauthorizedCallback } from '../shared/api/client/apiClient';
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
    
    // 인터셉터에서 401 에러 시 호출될 콜백 설정
    setUnauthorizedCallback(() => {
      console.log('Unauthorized callback 호출됨 - 인증 상태 변경');
      setIsAuthenticated(false);
      setShowSplash(true);
    });
  }, []);

  const checkAuthState = async () => {
    try {
      // JWT 토큰 체크는 하지만 인증 상태는 false로 설정 (카카오 로그인 후 팀선택으로 보내기 위함)
      const token = await tokenManager.getToken();
      const hasToken = !!token;
      console.log('토큰 상태:', { hasToken, tokenLength: token ? token.length : 0 });
      setIsAuthenticated(false); // 항상 스플래시 -> 카카오로그인 -> 팀선택 플로우
    } catch (error) {
      console.error('Auth check error ', error);
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoginSuccess = async (userInfo: any, accessToken: string) => {
    setShowSplash(false);
    setKakaoUserInfo(userInfo);
    setKakaoAccessToken(accessToken);
    
    // 서버에서 JWT 토큰을 받은 후에만 인증된 상태로 변경
    const token = await tokenManager.getToken();
    setIsAuthenticated(!!token);
  };

  const handleSignUpComplete = () => {
    console.log('handleSignUpComplete 호출됨');
    setIsAuthenticated(true);
  };

  // 로딩 중에는 아무것도 표시하지 않음
  if (isLoading) {
    return null;
  }

  if (showSplash) {
    return (
      <SplashScreen
        onAnimationComplete={() => {
          // 애니메이션 완료 후에도 스플래시 유지 (카카오 로그인 대기)
          // setShowSplash(false); // 이 줄을 주석처리하여 카카오 로그인 전까지 스플래시 유지
        }}
        onLoginSuccess={handleLoginSuccess}
      />
    );
  }

  return (
    <NavigationContainer ref={navigationRef} linking={linking}>
      {/* <StatusBar barStyle='light-content' backgroundColor={theme.colors.background} /> */}
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {isAuthenticated ? (
          <Stack.Screen name='MainTabs' component={MainNavigator} />
        ) : (
          <Stack.Screen name='AuthStack'>
            {() => <AuthNavigator onSignUpComplete={handleSignUpComplete} />}
          </Stack.Screen>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}

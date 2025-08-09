import React, { useEffect, useState } from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { NavigationContainer } from '@react-navigation/native';
import AuthNavigator from './AuthNavigator';
import MainNavigator from './MainNavigator';
import { linking } from './linking';
import SplashScreen from '../pages/splash/ui/SplashScreen';
import { navigationRef } from './navigationRefs';
import { usekakaoStore } from '../features/user-auth/model/kakaoStore';
import { useTokenStore } from '../shared/api/token/tokenStore';
import { useUserStore } from '../entities/user/model/userStore';
import { isErr, isOk } from '../shared/utils/result';
import { Alert } from 'react-native';
import { Token } from '../shared/api/token/tokenTypes';

const Stack = createStackNavigator();

export default function AppNavigator() {
  const [isLoading, setIsLoading] = useState(true);
  const [showSplash, setShowSplash] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isExistingUser, setIsExistingUser] = useState(false);

  const { setKakaoUserInfo, setKakaoAccessToken } = usekakaoStore();
  const { initializeTokens, refreshTokens, isTokenInitialized } = useTokenStore();
  const { initializeUser, setCurrentUser, isUserInitialized } = useUserStore();

  useEffect(() => {
    initializeApp();
  }, []);

  const initializeApp = async () => {
    try {
      await Promise.all([initializeTokens(), initializeUser()]);
      await checkAuthState();
    } catch (error) {
      console.error('App initialization failed:', error);
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  };

  const checkAuthState = async () => {
    try {
      // 기존 사용자 여부 확인
      const hasUserResult = await useUserStore.getState().hasUser();
      if (isOk(hasUserResult)) {
        setIsExistingUser(hasUserResult.data);
      }

      const refreshResult = await refreshTokens();

      if (isOk(refreshResult) && refreshResult.data) {
        setIsAuthenticated(true);
      } else {
        setIsAuthenticated(false);

        if (isOk(hasUserResult) && hasUserResult.data) {
          await setCurrentUser(null);
          setIsExistingUser(false);
        }
      }
    } catch (error) {
      console.error('Auth check error ', error);
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoginSuccess = (userInfo: any, accessToken: string) => {
    setShowSplash(false);
    setKakaoUserInfo(userInfo);
    setKakaoAccessToken(accessToken);
  };

  const handleSignUpComplete = async (userInfo: any, tokens: Token) => {
    try {
      const setTokensResult = await useTokenStore.getState().setTokens(tokens.accessToken, tokens.refreshToken);

      if (isErr(setTokensResult)) {
        throw new Error('Token save failed');
      }

      await setCurrentUser(userInfo);
      setIsAuthenticated(true);
      setIsExistingUser(true);
    } catch (error) {
      console.error('Sign up completion failed:', error);
      Alert.alert('오류', '회원가입 완료 처리 중 오류가 발생했습니다.');
    }
  };

  if (showSplash) {
    return (
      <SplashScreen
        onAnimationComplete={() => {
          setShowSplash(false);
        }}
        onLoginSuccess={handleLoginSuccess}
      />
    );
  }

  return (
    <NavigationContainer ref={navigationRef} linking={linking}>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {isAuthenticated ? (
          <Stack.Screen name='MainTabs' component={MainNavigator} />
        ) : (
          <Stack.Screen name='AuthStack'>
            {() => <AuthNavigator onSignUpComplete={handleSignUpComplete} isExistingUser={isExistingUser} />}
          </Stack.Screen>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}

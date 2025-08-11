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
import { initializeApiClient } from '../shared/api/client/apiClient';
import { useTheme } from '../shared/team/ThemeContext';
import { findTeamById } from '../shared/team/teamTypes';
import { AttendanceVerificationScreen } from '../pages/attendance';

const Stack = createStackNavigator();

export default function AppNavigator() {
  const [showSplash, setShowSplash] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isExistingUser, setIsExistingUser] = useState(false);

  const { setKakaoUserInfo, setKakaoAccessToken } = usekakaoStore();
  const { initializeTokens, resetToken } = useTokenStore();
  const { initializeUser, setCurrentUser, reset } = useUserStore();
  const { setCurrentTeam } = useTheme();

  useEffect(() => {
    initializeApp();
  }, []);

  const testReset = () => {
    resetToken();
    reset();
  };

  const initializeApp = async () => {
    try {
      await Promise.all([initializeTokens(), initializeUser()]);
      //await testReset();
      initializeApiClient();
    } catch (error) {
      console.error('App initialization failed:', error);
      setIsAuthenticated(false);
    }
  };

  // 자동로그인 성공 시 호출
  const handleAutoLoginSuccess = () => {
    setIsAuthenticated(true);
    setIsExistingUser(true);
    setShowSplash(false);
  };

  // 카카오 로그인 성공 시 호출 (기존 사용자 여부 확인 필요)
  const handleLoginSuccess = async (userInfo: any, accessToken: string) => {
    try {
      setKakaoUserInfo(userInfo);
      setKakaoAccessToken(accessToken);

      // 기존 사용자인지 확인
      const hasUserResult = await useUserStore.getState().hasUser();
      const isExisting = isOk(hasUserResult) && hasUserResult.data;

      setIsExistingUser(isExisting);
      setShowSplash(false);

      // 기존 사용자라면 서버에 로그인 요청
      if (isExisting) {
        console.error('기존 사용자임');
        await handleExistingUserLogin(accessToken);
      }
    } catch (error) {
      console.error('Login success handling failed:', error);
      setShowSplash(false);
    }
  };

  // 기존 사용자 로그인 처리
  const handleExistingUserLogin = async (kakaoAccessToken: string) => {
    try {
      const { authApi } = await import('../features/user-auth/api/authApi');

      const loginResult = await authApi.login({
        accessToken: kakaoAccessToken,
      });

      if (isOk(loginResult)) {
        const { userProfile, tokens } = loginResult.data;

        const setTokensResult = await useTokenStore.getState().setTokens(tokens);

        if (isErr(setTokensResult)) {
          throw new Error('Token save failed');
        }

        // 사용자 정보 및 팀 테마 설정
        await handleSetUserAndTeam(userProfile);
        setIsAuthenticated(true);

        console.log('Existing user login successful');
      } else {
        throw new Error(loginResult.error.message);
      }
    } catch (error) {
      console.error('Existing user login failed:', error);
      Alert.alert('로그인 실패', '자동 로그인에 실패했습니다. 다시 로그인해주세요.');
      // 이게 안되면 뭐먹고 사냐?
    }
  };

  const handleSetUserAndTeam = async (userInfo: any) => {
    await setCurrentUser(userInfo);

    // 사용자 정보로부터 팀 설정
    if (userInfo.teamId) {
      const team = findTeamById(userInfo.teamId);
      if (team) {
        setCurrentTeam(team);
      }
    }
  };

  // 회원가입 완료 시 호출
  const handleSignUpComplete = async (userInfo: any, tokens: Token) => {
    try {
      const setTokensResult = await useTokenStore.getState().setTokens(tokens);

      if (isErr(setTokensResult)) {
        throw new Error('Token save failed');
      }

      await handleSetUserAndTeam(userInfo);
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
        onAnimationComplete={() => setShowSplash(false)}
        onLoginSuccess={handleLoginSuccess}
        onAutoLoginSuccess={handleAutoLoginSuccess}
      />
    );
  }

  return (
    <NavigationContainer ref={navigationRef} linking={linking}>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {isAuthenticated ? (
          <>
            <Stack.Screen name='MainTabs' component={MainNavigator} />
            <Stack.Screen name='AttendanceVerification' component={AttendanceVerificationScreen} />
          </>
        ) : (
          <Stack.Screen name='AuthStack'>
            {() => <AuthNavigator onSignUpComplete={handleSignUpComplete} isExistingUser={isExistingUser} />}
          </Stack.Screen>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}

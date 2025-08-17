import React, { useEffect, useState, useMemo, useCallback } from 'react';
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
import { initializeApiClient, setUnauthorizedCallback } from '../shared/api/client/apiClient';
import { useTheme } from '../shared/team/ThemeContext';
import { findTeamById } from '../shared/team/teamTypes';
import { AttendanceVerificationScreen } from '../pages/attendance';
import { WatchChatModalScreen } from '../pages/match-chat/WatchChatModalScreen';

const Stack = createStackNavigator();

// 컴포넌트 외부에서 selector 정의 (안정적인 참조)
const selectHasToken = (state: any) => !!state.refreshToken;
const selectHasUser = (state: any) => !!state.currentUser;

export default function AppNavigator() {
  const [showSplash, setShowSplash] = useState(true);
  const [isExistingUser, setIsExistingUser] = useState(false);

  const { setKakaoUserInfo, setKakaoAccessToken } = usekakaoStore();
  const { initializeTokens, resetToken } = useTokenStore();
  const { initializeUser, setCurrentUser, reset } = useUserStore();
  const { setCurrentTeam } = useTheme();

  const hasToken = useTokenStore(selectHasToken);
  const hasUser = useUserStore(selectHasUser);
  const isAuthenticated = useMemo(() => {
    return hasToken && hasUser;
  }, [hasToken, hasUser]);

  // 로그아웃 처리 콜백
  const handleLogout = useCallback(() => {
    resetToken();
    reset();
    setIsExistingUser(false);
    setShowSplash(false);
  }, [resetToken, reset]);

  // 앱 초기화 함수
  const initializeApp = useCallback(async () => {
    try {
      // 순차적으로 초기화 (의존성 때문에)
      await initializeTokens();
      await initializeUser();
      // await testReset();
      await initializeApiClient();
      setUnauthorizedCallback(handleLogout);
      console.log('✅ [AppNavigator] 앱 초기화 완료');
    } catch (error) {
      console.error('❌ [AppNavigator] 앱 초기화 실패:', error);
      // 인증 상태는 Zustand 상태에서 자동으로 계산됨
    }
  }, [initializeTokens, initializeUser, handleLogout]);

  useEffect(() => {
    initializeApp();
  }, [initializeApp]);

  const testReset = () => {
    resetToken();
    reset();
  };

  // 자동로그인 성공 시 호출
  const handleAutoLoginSuccess = () => {
    setIsExistingUser(true);
    setShowSplash(false);
  };

  // 카카오 로그인 성공 시 호출 (기존 사용자 여부 확인 필요)
  const handleLoginSuccess = async (userInfo: any, accessToken: string) => {
    try {
      setKakaoUserInfo(userInfo);
      setKakaoAccessToken(accessToken);

      // 먼저 서버에 로그인을 시도해서 기존 사용자인지 확인
      const { authApi } = await import('../features/user-auth/api/authApi');

      const loginResult = await authApi.login({
        accessToken: accessToken,
      });

      if (isOk(loginResult)) {
        // 기존 사용자 - 로그인 성공
        const { userProfile, tokens } = loginResult.data;

        const setTokensResult = await useTokenStore.getState().setTokens(tokens);
        if (isErr(setTokensResult)) {
          throw new Error('Token save failed');
        }

        await handleSetUserAndTeam(userProfile);
        setIsExistingUser(true);
        setShowSplash(false);

        console.log('Existing user login successful');
      } else {
        // 404 에러는 신규 사용자를 의미하므로 정상 플로우로 처리
        if (loginResult.error?.status === 404 || loginResult.error?.message?.includes('존재하지 않는 사용자')) {
          console.log('New user detected - proceeding to signup flow');
          setIsExistingUser(false);
          setShowSplash(false);
        } else {
          // 다른 에러는 실제 에러로 처리
          console.error('Login failed with unexpected error:', loginResult.error);
          setIsExistingUser(false);
          setShowSplash(false);
        }
      }
    } catch (error) {
      console.error('Login success handling failed:', error);
      setIsExistingUser(false);
      setShowSplash(false);
    }
  };

  // 유저로부터 팀 상태 정보 설정
  const handleSetUserAndTeam = async (userInfo: any) => {
    await setCurrentUser(userInfo);
    console.log('userInfo', userInfo);

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
      setIsExistingUser(true);

      Alert.alert('회원가입 완료', '환영합니다! 빠팅과 함께 야구를 즐겨보세요.', [
        {
          text: '확인',
          onPress: () => {
            // 메인 화면으로 전환을 위해 인증 상태 업데이트
            // isAuthenticated가 true가 되면 자동으로 MainNavigator로 전환됨
          },
        },
      ]);
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
            <Stack.Screen
              name='AttendanceVerification'
              component={AttendanceVerificationScreen}
              options={{
                presentation: 'modal',
                headerShown: true,
                headerTitle: '직관 인증',
              }}
            />
            <Stack.Screen
              name='WatchChatModal'
              component={WatchChatModalScreen}
              options={{
                presentation: 'modal',
                headerShown: false,
              }}
            />
          </>
        ) : (
          <Stack.Screen name='AuthStack'>
            {(props) => (
              <AuthNavigator {...props} onSignUpComplete={handleSignUpComplete} isExistingUser={isExistingUser} />
            )}
          </Stack.Screen>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}

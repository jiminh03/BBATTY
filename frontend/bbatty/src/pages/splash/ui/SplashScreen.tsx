import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  Animated,
  Dimensions,
  TouchableOpacity,
  AppState,
  Alert,
  Image,
  ImageBackground,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { styles } from './SplashScreen.styles';
import { screen } from '../../../shared';
import { useTokenStore } from '../../../shared/api/token/tokenStore';
import { useUserStore } from '../../../entities/user/model/userStore';
import { isErr, isOk } from '../../../shared/utils/result';
import { useTheme } from '../../../shared/team/ThemeContext';
import { findTeamById } from '../../../shared/team/teamTypes';
import { usekakaoStore } from '../../../features/user-auth/model/kakaoStore';

interface SplashScreenProps {
  onAnimationComplete?: () => void;
  onLoginSuccess?: (userInfo: any, accessToken: string) => void;
  onAutoLoginSuccess?: () => void;
}

const SplashScreen: React.FC<SplashScreenProps> = ({ onAnimationComplete, onLoginSuccess, onAutoLoginSuccess }) => {
  const insets = useSafeAreaInsets();
  const { width } = screen;

  const [shouldShowLogin, setShouldShowLogin] = useState(false);

  // 애니메이션 값들
  const ballPosition = useRef(new Animated.Value(-100)).current;
  const ballRotation = useRef(new Animated.Value(0)).current;
  const buttonOpacity = useRef(new Animated.Value(0)).current;
  const buttonTranslateY = useRef(new Animated.Value(20)).current;

  const { refreshTokens, hasRefreshToken, isRefreshTokenExpired, resetToken } = useTokenStore();
  const { hasUser, getCurrentUser, reset: resetUser } = useUserStore();
  const { setCurrentTeam } = useTheme();
  const { setKakaoUserInfo, setKakaoAccessToken } = usekakaoStore();

  useEffect(() => {
    initializeApp();
  }, []);

  const initializeApp = async () => {
    await initializeKakao();
    // AppNavigator에서 초기화가 완료될 때까지 잠시 대기
    await new Promise((resolve) => setTimeout(resolve, 100));
    await checkAutoLogin();
  };

  const initializeKakao = async () => {
    try {
      const { initializeKakaoSDK } = await import('@react-native-kakao/core');
      await initializeKakaoSDK('f3cdad34b10d9d1bcb6b42cde54d015d');
    } catch (error) {
      console.error('카카오 초기화 실패:', error);
    }
  };

  const checkAutoLogin = async () => {
    try {
      // 토큰 스토어의 초기화 상태 확인
      const { isTokenInitialized } = useTokenStore.getState();
      if (!isTokenInitialized) {
        console.log('⏳ [SplashScreen] 토큰 스토어 초기화 대기 중...');
        // 최대 3초까지 초기화 완료 대기
        let waitCount = 0;
        while (!useTokenStore.getState().isTokenInitialized && waitCount < 30) {
          await new Promise((resolve) => setTimeout(resolve, 100));
          waitCount++;
        }

        if (!useTokenStore.getState().isTokenInitialized) {
          console.log('⚠️ [SplashScreen] 토큰 스토어 초기화 시간 초과');
        }
      }

      // 1. 사용자 정보 및 토큰 확인
      const hasTokens = hasRefreshToken();
      
      // 토큰이 없으면 일찍 종료
      if (!hasTokens) {
        console.log('🔴 [SplashScreen] No tokens found, skipping auto login');
        setShouldShowLogin(true);
        startAnimationWithLogin();
        return;
      }
      
      const hasUserResult = await hasUser();
      const userExists = isOk(hasUserResult) && hasUserResult.data;

      console.log('🔍 [SplashScreen] 자동로그인 체크:', {
        userExists,
        hasTokens,
        isRefreshTokenExpired: isRefreshTokenExpired(),
        isTokenInitialized: useTokenStore.getState().isTokenInitialized,
      });

      // 2. 토큰 유효성 확인 및 갱신 시도
      if (userExists && hasTokens) {
        // Refresh 토큰 만료 여부 추가 체크
        if (isRefreshTokenExpired()) {
          console.log('🔴 [SplashScreen] Refresh token expired, requiring login');
          setShouldShowLogin(true);
          startAnimationWithLogin();
          return;
        }

        console.log('🔄 [SplashScreen] 토큰 갱신 시도');
        const refreshResult = await refreshTokens();

        if (isOk(refreshResult) && refreshResult.data) {
          // 토큰 갱신 성공 - 이제 실제 사용자 존재 여부 확인
          console.log('🔄 [SplashScreen] 토큰 갱신 성공, 사용자 존재 여부 확인 중...');
          
          try {
            // 프로필 API 호출로 사용자 존재 여부 확인
            const { profileApi } = await import('../../../features/user-profile/api/profileApi');
            const profileResult = await profileApi.getProfile();
            
            if (isOk(profileResult)) {
              // 사용자 존재 확인 - 자동로그인 성공
              const currentUser = getCurrentUser();
              if (currentUser?.teamId) {
                const team = findTeamById(currentUser.teamId);
                if (team) {
                  setCurrentTeam(team);
                }
              }

              console.log('✅ [SplashScreen] Auto login successful');
              startAnimationAndComplete();
              return;
            } else {
              // 프로필 조회 실패 - 탈퇴된 사용자
              console.log('🚫 [SplashScreen] User profile not found - user may have been deleted');
              throw new Error('User profile not found');
            }
          } catch (profileError) {
            console.log('❌ [SplashScreen] Profile check failed:', profileError);
            
            // 탈퇴된 사용자로 판단 - 상태 정리
            console.log('🧹 [SplashScreen] Clearing user state - user may have withdrawn');
            resetToken();
            await resetUser();
            setKakaoUserInfo(null);
            setKakaoAccessToken(null);
          }
        } else {
          console.log('❌ [SplashScreen] Token refresh failed:', refreshResult.error);
        }
      } else {
        console.log('🔴 [SplashScreen] No user or tokens found');
      }

      // 자동로그인 실패 - 로그인 버튼 표시
      setShouldShowLogin(true);
      startAnimationWithLogin();
    } catch (error) {
      console.error('❌ [SplashScreen] Auto login check failed:', error);
      setShouldShowLogin(true);
      startAnimationWithLogin();
    }
  };

  const startAnimationAndComplete = () => {
    const animationSequence = Animated.sequence([
      // 야구공 애니메이션
      Animated.parallel([
        Animated.timing(ballPosition, {
          toValue: width + 100,
          duration: 1875,
          useNativeDriver: true,
        }),
        Animated.timing(ballRotation, {
          toValue: 3,
          duration: 1875,
          useNativeDriver: true,
        }),
      ]),

      // 잠시 대기 후 자동으로 메인으로 이동
      Animated.delay(800),
    ]);

    animationSequence.start(() => {
      if (onAutoLoginSuccess) {
        onAutoLoginSuccess();
      } else {
        onAnimationComplete?.();
      }
    });
  };

  const startAnimationWithLogin = () => {
    const animationSequence = Animated.sequence([
      // 야구공 애니메이션
      Animated.parallel([
        Animated.timing(ballPosition, {
          toValue: width + 100,
          duration: 1875,
          useNativeDriver: true,
        }),
        Animated.timing(ballRotation, {
          toValue: 3,
          duration: 1875,
          useNativeDriver: true,
        }),
      ]),

      // 잠시 대기
      Animated.delay(500),

      // 로그인 버튼 표시
      Animated.parallel([
        Animated.timing(buttonOpacity, {
          toValue: 1,
          duration: 300,
          useNativeDriver: true,
        }),
        Animated.timing(buttonTranslateY, {
          toValue: 0,
          duration: 300,
          useNativeDriver: true,
        }),
      ]),
    ]);

    animationSequence.start();
  };

  // 야구공 회전 보간
  const ballRotate = ballRotation.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  });

  const handleKakaoLoginPress = async () => {
    try {
      const { login } = await import('@react-native-kakao/user');
      const kakaoData = await login();

      const response = await fetch('https://kapi.kakao.com/v2/user/me', {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${kakaoData.accessToken}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('사용자 정보를 가져올 수 없습니다.');
      }

      const userInfo = await response.json();

      console.log(kakaoData.accessToken);
      if (onLoginSuccess) {
        onLoginSuccess(userInfo, kakaoData.accessToken);
      } else {
        onAnimationComplete?.();
      }
    } catch (error: any) {
      Alert.alert('로그인 실패', error.message || '카카오 로그인에 실패했습니다. 다시 시도해주세요.', [
        { text: '확인' },
      ]);
    }
  };

  return (
    <ImageBackground
      source={require('../../../../assets/Splash.jpg')}
      style={[styles.container, { paddingTop: insets.top }]}
      resizeMode='cover'
    >
      {/* 날아오는 야구공 */}
      <Animated.View
        style={[
          styles.baseball,
          {
            transform: [{ translateX: ballPosition }, { rotate: ballRotate }],
          },
        ]}
      >
        <Image
          source={require('../../../../assets/baseball-ball.png')}
          style={styles.baseballImage}
          resizeMode='contain'
        />
      </Animated.View>

      {/* 카카오 로그인 버튼 - 조건부 렌더링 */}
      {shouldShowLogin && (
        <Animated.View
          style={[
            styles.loginButtonContainer,
            {
              opacity: buttonOpacity,
              transform: [{ translateY: buttonTranslateY }],
            },
          ]}
        >
          <TouchableOpacity style={styles.loginButton} onPress={handleKakaoLoginPress}>
            <Text style={styles.kakaoIcon}>💬</Text>
            <Text style={styles.loginButtonText}>카카오 로그인</Text>
          </TouchableOpacity>
        </Animated.View>
      )}
    </ImageBackground>
  );
};

export default SplashScreen;

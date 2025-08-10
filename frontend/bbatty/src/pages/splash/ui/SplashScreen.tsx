import React, { useEffect, useRef, useState } from 'react';
import { View, Text, Animated, Dimensions, TouchableOpacity, AppState, Alert } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { styles } from './SplashScreen.styles';
import { screen } from '../../../shared';
import { useTokenStore } from '../../../shared/api/token/tokenStore';
import { useUserStore } from '../../../entities/user';
import { isErr, isOk } from '../../../shared/utils/result';

interface SplashScreenProps {
  onAnimationComplete?: () => void;
  onLoginSuccess?: (userInfo: any, accessToken: string) => void;
  onAutoLoginSuccess?: () => void; // 자동로그인 성공 콜백
}

const SplashScreen: React.FC<SplashScreenProps> = ({ onAnimationComplete, onLoginSuccess, onAutoLoginSuccess }) => {
  const insets = useSafeAreaInsets();
  const { width } = screen;

  const [shouldShowLogin, setShouldShowLogin] = useState(false);
  const [isCheckingAuth, setIsCheckingAuth] = useState(true);

  // 애니메이션 값들
  const ballPosition = useRef(new Animated.Value(-100)).current;
  const ballRotation = useRef(new Animated.Value(0)).current;
  const tiOpacity = useRef(new Animated.Value(1)).current;
  const tingOpacity = useRef(new Animated.Value(0)).current;
  const buttonOpacity = useRef(new Animated.Value(0)).current;
  const buttonTranslateY = useRef(new Animated.Value(20)).current;

  const { refreshTokens, hasRefreshToken, isRefreshTokenExpired } = useTokenStore();
  const { hasUser, getCurrentUser } = useUserStore();

  useEffect(() => {
    initializeApp();
  }, []);

  const initializeApp = async () => {
    await initializeKakao();
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
      setIsCheckingAuth(true);

      // 1. 사용자 정보 확인
      const hasUserResult = await hasUser();
      const userExists = isOk(hasUserResult) && hasUserResult.data;

      // 2. 토큰 유효성 확인 및 갱신 시도
      if (userExists && hasRefreshToken()) {
        // Refresh 토큰 만료 여부 추가 체크
        if (isRefreshTokenExpired()) {
          console.log('Refresh token expired, requiring login');
          setShouldShowLogin(true);
          startAnimationWithLogin();
          return;
        }
        const refreshResult = await refreshTokens();

        if (isOk(refreshResult) && refreshResult.data) {
          // 자동로그인 성공 - 애니메이션 후 메인으로 이동
          console.log('Auto login successful');
          startAnimationAndComplete();
          return;
        } else {
          console.log('Token refresh failed:', refreshResult.error);
        }
      }

      // 자동로그인 실패 - 로그인 버튼 표시
      setShouldShowLogin(true);
      startAnimationWithLogin();
    } catch (error) {
      console.error('Auto login check failed:', error);
      setShouldShowLogin(true);
      startAnimationWithLogin();
    } finally {
      setIsCheckingAuth(false);
    }
  };

  const startAnimationAndComplete = () => {
    const animationSequence = Animated.sequence([
      // 야구공 애니메이션
      Animated.parallel([
        Animated.timing(ballPosition, {
          toValue: width / 2 + 20,
          duration: 1500,
          useNativeDriver: true,
        }),
        Animated.timing(ballRotation, {
          toValue: 3,
          duration: 1500,
          useNativeDriver: true,
        }),
      ]),

      // 티를 팅으로 교체
      Animated.sequence([
        Animated.timing(tiOpacity, {
          toValue: 0,
          duration: 200,
          useNativeDriver: true,
        }),
        Animated.timing(tingOpacity, {
          toValue: 1,
          duration: 200,
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
          toValue: width / 2 + 20,
          duration: 1500,
          useNativeDriver: true,
        }),
        Animated.timing(ballRotation, {
          toValue: 3,
          duration: 1500,
          useNativeDriver: true,
        }),
      ]),

      // 티를 팅으로 교체
      Animated.sequence([
        Animated.timing(tiOpacity, {
          toValue: 0,
          duration: 200,
          useNativeDriver: true,
        }),
        Animated.timing(tingOpacity, {
          toValue: 1,
          duration: 200,
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
      // 로그인 성공 콜백 호출
      if (onLoginSuccess) {
        onLoginSuccess(userInfo, kakaoData.accessToken);
      } else {
        // 애니메이션 완료 콜백 호출
        onAnimationComplete?.();
      }
    } catch (error: any) {
      // 사용자에게 에러 메시지 표시
      Alert.alert('로그인 실패', error.message || '카카오 로그인에 실패했습니다. 다시 시도해주세요.', [
        { text: '확인' },
      ]);
    }
  };

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* 배경 야구공 실밥 패턴 */}
      <View style={styles.backgroundPattern}>
        {[...Array(6)].map((_, index) => (
          <View
            key={index}
            style={[
              styles.stitchLine,
              {
                top: `${index * 20}%`,
                transform: [{ rotate: '45deg' }],
              },
            ]}
          />
        ))}
      </View>

      {/* 메인 콘텐츠 */}
      <View style={styles.content}>
        <View style={styles.titleContainer}>
          <Text style={styles.titleText}>빠</Text>

          {/* 티/팅 전환 */}
          <View style={styles.letterContainer}>
            <Animated.Text style={[styles.titleText, styles.tiText, { opacity: tiOpacity }]}>티</Animated.Text>
            <Animated.Text style={[styles.titleText, styles.tingText, { opacity: tingOpacity }]}>팅</Animated.Text>

            {/* 팅 아래 밑줄 */}
            <Animated.View style={[styles.underline, { opacity: tingOpacity }]} />
          </View>
        </View>

        {/* 애니메이션 서브타이틀 */}
        <Text style={styles.subtitle}>애니메이션</Text>
      </View>

      {/* 날아오는 야구공 */}
      <Animated.View
        style={[
          styles.baseball,
          {
            transform: [{ translateX: ballPosition }, { rotate: ballRotate }],
          },
        ]}
      >
        <View style={styles.baseballStitch} />
        <View style={[styles.baseballStitch, styles.baseballStitchVertical]} />
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
    </View>
  );
};

export default SplashScreen;

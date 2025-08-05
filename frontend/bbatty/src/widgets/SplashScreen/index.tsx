import React, { useEffect, useRef } from 'react';
import { View, Text, Animated, Dimensions, TouchableOpacity } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useKakaoLogin } from '../../entities/auth/hooks/useKakaoLogin';
import { login } from '@react-native-seoul/kakao-login';
import { styles } from './styles';

interface SplashScreenProps {
  onAnimationComplete?: () => void;
}

const SplashScreen: React.FC<SplashScreenProps> = ({ onAnimationComplete }) => {
  const insets = useSafeAreaInsets();
  const { width } = Dimensions.get('window');

  console.log('???');

  // 애니메이션 값들
  const ballPosition = useRef(new Animated.Value(-100)).current;
  const ballRotation = useRef(new Animated.Value(0)).current;
  const tiOpacity = useRef(new Animated.Value(1)).current;
  const tingOpacity = useRef(new Animated.Value(0)).current;
  const buttonOpacity = useRef(new Animated.Value(0)).current;
  const buttonTranslateY = useRef(new Animated.Value(20)).current;

  useEffect(() => {
    // 애니메이션 시퀀스
    const animationSequence = Animated.sequence([
      // 1. 야구공이 날아오면서 회전
      Animated.parallel([
        Animated.timing(ballPosition, {
          toValue: width / 2 + 20, // 티 글자 위치
          duration: 1500,
          useNativeDriver: true,
        }),
        Animated.timing(ballRotation, {
          toValue: 3, // 3바퀴 회전
          duration: 1500,
          useNativeDriver: true,
        }),
      ]),

      // 2. 티를 팅으로 교체
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

      // 3. 잠시 대기
      Animated.delay(500),

      // 4. 로그인 버튼 표시
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

    // 애니메이션 시작
    animationSequence.start(() => {
      // 애니메이션 완료 후 1초 대기
      setTimeout(() => {
        onAnimationComplete?.();
      }, 1000);
    });
  }, []);

  const handleKakaoPress = async () => {
    try {
      //  카카오 SDK를 통한 카카오 로그인
      const token = await login();
      const kakaoAccessToken = token.accessToken;

      // 2단계: 받은 카카오 토큰을 우리 서버로 전송
      await useKakaoLogin(kakaoAccessToken);
    } catch (error) {
      console.error('카카오 로그인 실패:', error);
    }
  };

  // 야구공 회전 보간
  const ballRotate = ballRotation.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  });

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

      {/* 카카오 로그인 버튼 */}
      <Animated.View
        style={[
          styles.loginButtonContainer,
          {
            opacity: buttonOpacity,
            transform: [{ translateY: buttonTranslateY }],
          },
        ]}
      >
        <TouchableOpacity style={styles.loginButton} onPress={handleKakaoPress}>
          <Text style={styles.kakaoIcon}>💬</Text>
          <Text style={styles.loginButtonText}>카카오 로그인</Text>
        </TouchableOpacity>
      </Animated.View>
    </View>
  );
};

export default SplashScreen;

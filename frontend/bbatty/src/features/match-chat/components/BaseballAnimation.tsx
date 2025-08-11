import React, { useEffect, useRef } from 'react';
import {
  View,
  StyleSheet,
  Animated,
  Dimensions,
  Text,
  ActivityIndicator,
} from 'react-native';
import { useThemeColor } from '../../../shared/team/ThemeContext';

const { width, height } = Dimensions.get('window');

interface BaseballAnimationProps {
  onAnimationComplete: () => void;
  onNavigate: () => void;
}

export const BaseballAnimation: React.FC<BaseballAnimationProps> = ({
  onAnimationComplete,
  onNavigate,
}) => {
  const themeColor = useThemeColor();
  
  const fadeIn = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    // 페이드 인
    Animated.timing(fadeIn, {
      toValue: 1,
      duration: 300,
      useNativeDriver: true,
    }).start();

    // API 호출 시작
    onNavigate();

    // 2초 후 페이드 아웃
    const timer = setTimeout(() => {
      Animated.timing(fadeIn, {
        toValue: 0,
        duration: 300,
        useNativeDriver: true,
      }).start(() => {
        onAnimationComplete();
      });
    }, 2000);

    return () => clearTimeout(timer);
  }, []);

  return (
    <Animated.View 
      style={[
        styles.container, 
        { 
          backgroundColor: themeColor,
          opacity: fadeIn 
        }
      ]}
    >
      <View style={styles.content}>
        <ActivityIndicator size="large" color="#ffffff" />
        <Text style={styles.text}>접속 중...</Text>
      </View>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 1000,
  },
  content: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    fontSize: 18,
    fontWeight: '600',
    color: '#ffffff',
    marginTop: 16,
    textAlign: 'center',
  },
});
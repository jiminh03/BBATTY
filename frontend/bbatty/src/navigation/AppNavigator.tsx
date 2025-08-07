import React, { useEffect, useState } from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { createNavigationContainerRef, NavigationContainer } from '@react-navigation/native';
import { StatusBar } from 'react-native';
import { useTheme } from '../shared/styles';
import { tokenManager } from '../shared';
import { RootStackParamList } from './types';
import AuthNavigator from './AuthNavigator';
import MainNavigator from './MainNavigator';
import { linking } from './linking';
import SplashScreen from '../pages/splash/ui';
import { navigationRef } from './navigationRefs';

const Stack = createStackNavigator();

export default function AppNavigator() {
  const [userInfo, setUserInfo] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showSplash, setShowSplash] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const { theme } = useTheme();

  useEffect(() => {
    checkAuthState();
  }, []);

  const checkAuthState = async () => {
    try {
      const token = await tokenManager.getToken();
      setIsAuthenticated(!!token);
    } catch (error) {
      console.error('Auth check error ', error);
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoginSuccess = (userInfo: any) => {
    setIsAuthenticated(true);
    setShowSplash(false);
    setUserInfo(userInfo);

    // //로그인 성공 시 팀 선택 화면으로 이동
    // navigationRef.navigate('AuthStack', {
    //   screen: 'TeamSelect',
    //   params: {
    //     nickname: userInfo.properties?.nickname,
    //   },
    // });
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
        <Stack.Screen name='AuthStack' children={() => <AuthNavigator userInfo={userInfo} />} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

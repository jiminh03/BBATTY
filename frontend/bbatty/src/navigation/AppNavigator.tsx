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
      setIsAuthenticated(!!token);
    } catch (error) {
      console.error('Auth check error ', error);
      setIsAuthenticated(false);
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

  const handleSignUpComplete = () => {
    setIsAuthenticated(true);
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

import React, { useEffect, useState } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { StatusBar } from 'react-native';
import { useTheme } from '../shared/styles';
import { tokenManager } from '../shared';
import { RootStackParamList } from './types';
import AuthNavigator from './AuthNavigator';
import MainNavigator from './MainNavigator';
// import TeamSelectModal from '@/screens/modals/TeamSelectModal';
// import ImageViewerModal from '@/screens/modals/ImageViewerModal';
// import ReportModal from '@/screens/modals/ReportModal';
// import SplashScreen from '@/widgets/SplashScreen';
// import { SplashS }
import { navigationRef } from './naviagtionRefs';
import { linking } from './linking';
import SplashScreen from '../pages/SplashScreen';

const Stack = createStackNavigator<RootStackParamList>();

export default function AppNavigator() {
  const [isLoading, setIsLoading] = useState(true);
  const [showSplash, setShowSplash] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const { theme } = useTheme();

  /*
  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      // 저장된 토큰 확인
      const token = await tokenManager.getToken();
      setIsAuthenticated(!!token);
    } catch (error) {
      console.error('인증 상태 확인 실패:', error);
    } finally {
      setIsLoading(false);
    }
  };*/

  if (isLoading) {
    // return <SplashScreen />;
  }

  return (
    <SplashScreen
      onAnimationComplete={() => {
        setShowSplash(false);
      }}
    ></SplashScreen>
  );
}

/*

    <>
      <StatusBar
        barStyle={theme.colors.primary === '#FFFFFF' ? 'dark-content' : 'light-content'}
        backgroundColor={theme.colors.primary}
      />
      <NavigationContainer
        ref={navigationRef}
        linking={linking}
        // theme={{
        //   dark: false,
        //   colors: {
        //     primary: theme.colors.primary,
        //     background: theme.colors.background,
        //     card: theme.colors.surface,
        //     text: theme.colors.text.primary,
        //     border: theme.colors.border,
        //     notification: theme.colors.primary,
        //   },
        //   fonts: {
        //     regular:,
        //     medium,
        //     bold,
        //     heavy,
        //   },
        // }}
      >
        <Stack.Navigator
          screenOptions={{
            headerShown: false,
            cardStyle: { backgroundColor: theme.colors.background },
          }}
        >
          <>
            { 로그인 전 화면들 }
            <Stack.Screen name='AuthStack' component={AuthNavigator} />
          </>
        </Stack.Navigator>
      </NavigationContainer>
    </>
*/

/*

          {isAuthenticated ? (
            <>
              <Stack.Screen name='MainTabs' component={MainNavigator} />

              <Stack.Screen
                name='TeamSelectModal'
                component={TeamSelectModal}
                options={{
                  presentation: 'modal',
                  headerShown: true,
                  headerTitle: '팀 선택',
                }}
              />
              <Stack.Screen
                name='ImageViewerModal'
                component={ImageViewerModal}
                options={{
                  presentation: 'modal',
                  headerShown: false,
                }}
              />
              <Stack.Screen
                name='ReportModal'
                component={ReportModal}
                options={{
                  presentation: 'modal',
                  headerShown: true,
                  headerTitle: '신고하기',
                }}
              />
            </>
          ) : (
            */

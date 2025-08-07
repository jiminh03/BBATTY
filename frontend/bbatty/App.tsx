// App.tsx
import React, { useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { initializeApiClient } from './src/shared';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ThemeProvider } from './src/shared/styles';
import AppNavigator from './src/navigation/AppNavigator';
/*
import { ErrorBoundary } from '@/shared/components/ErrorBoundary';
import { ToastProvider } from '@/shared/components/ToastProvider';
import { LoadingProvider } from '@/shared/components/LoadingProvider';
*/

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnReconnect: true,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
});

export default function App() {
  /*
  const [isAppReady, setIsAppReady] = React.useState(false);

  useEffect(() => {
    initializeApp();
  }, []);

  const initializeApp = async () => {
    try {
      await initializeApiClient();

      await AppInitService.checkNetworkConnection();

      await AppInitService.clearOldCache();

      setIsAppReady(true);
    } catch (error) {
      console.log('앱 초기화 실패: ', error);

      //초기화되도 일단 앱은 실행
      setIsAppReady(true);
    }
  };

  if (!isAppReady) {
    // 스플래쉬 화면 ㄱㄱ
    return null;
  }
*/
  useEffect(() => {
    initializeApiClient();
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider>
            {/* <ErrorBoundary>
              <LoadingProvider>
                <ToastProvider> */}
            <AppNavigator />
            {/* </ToastProvider>
              </LoadingProvider>
            </ErrorBoundary> */}
          </ThemeProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
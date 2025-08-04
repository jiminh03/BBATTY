import { useEffect } from 'react';
import * as Linking from 'expo-linking';
import { navigationRef } from '../../navigation/naviagtionRefs';

export const useDeepLink = () => {
  useEffect(() => {
    // 앱이 실행 중일 때 딥링크 처리
    const subscription = Linking.addEventListener('url', handleDeepLink);

    // 앱이 종료 상태에서 딥링크로 실행된 경우
    Linking.getInitialURL().then((url) => {
      if (url) {
        handleDeepLink({ url });
      }
    });

    return () => subscription.remove();
  }, []);

  const handleDeepLink = ({ url }: { url: string }) => {
    if (!navigationRef.isReady()) {
      // 준비될때까지 재귀
      setTimeout(() => handleDeepLink({ url }), 100);
      return;
    }

    const { hostname, path, queryParams } = Linking.parse(url);

    // 딥링크 라우팅 처리
    switch (hostname) {
      case 'chat':
        if (path && path.length > 0) {
          const roomId = path[0];
          navigationRef.navigate('MainTabs', {
            screen: 'ChatStack',
            params: {
              screen: 'ChatRoom',
              params: {
                roomId,
                roomType: 'match',
              },
            },
          });
        }
        break;

      case 'post':
        if (path && path.length > 0) {
          const postId = path[0];
          navigationRef.navigate('MainTabs', {
            screen: 'HomeStack',
            params: {
              screen: 'Home',
            },
          });
        }
        break;

      /*
      case 'attendance':
        if (queryParams.gameId && queryParams.stadiumId) {
          navigationRef.navigate('MainTabs', {
            screen: 'GameStack',
            params: {
              screen: 'AttendanceVerify',
              params: {
                gameId: queryParams.gameId as string,
                stadiumId: queryParams.stadiumId as string,
              },
            },
          });
        }
        break;
        */

      default:
        console.warn('알 수 없는 딥링크:', url);
    }
  };
};

import { LinkingOptions } from '@react-navigation/native';
import * as Linking from 'expo-linking';
import { RootStackParamList } from './types';

export const linking: LinkingOptions<RootStackParamList> = {
  prefixes: ['bbatty://', Linking.createURL('/')],
  config: {
    screens: {
      AuthStack: {
        screens: {
          Landing: 'welcome',
          Login: 'login',
          SignUp: 'signup',
          TeamSelect: 'team-select',
        },
      },
      MainTabs: {
        screens: {
          HomeStack: {
            screens: {
              Home: 'home',
              Notifications: 'notifications',
              Search: 'search',
            },
          },
          ChatStack: {
            screens: {
              ChatList: 'chats',
              ChatRoom: 'chat/:roomId',
              MatchingCreate: 'matching/create',
              MatchingDetail: 'matching/:matchingId',
            },
          },
          ExploreStack: {
            screens: {
              CommunityHome: 'community',
              PostList: 'posts',
              PostDetail: 'post/:postId',
            },
          },
          MyPageStack: {
            screens: {
              MyPage: 'mypage',
              Profile: 'profile/:userId',
              Settings: 'settings',
            },
          },
        },
      },
      // TeamSelectModal: 'modal/team-select',
      // ImageViewerModal: 'modal/image-viewer',
      // ReportModal: 'modal/report',
    },
  },

  // 딥링크 처리 전 실행되는 함수
  async getInitialURL() {
    // 앱이 닫혀있을 때 딥링크로 열린 경우
    const url = await Linking.getInitialURL();
    return url;
  },

  // 앱이 열려있을 때 딥링크 처리
  subscribe(listener) {
    const subscription = Linking.addEventListener('url', ({ url }) => {
      listener(url);
    });

    return () => subscription.remove();
  },
};

import { NavigatorScreenParams } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';

// 루트 스택 파라미터
export type RootStackParamList = {
  // 인증 스택
  AuthStack: NavigatorScreenParams<AuthStackParamList>;
  // 메인 탭
  MainTabs: NavigatorScreenParams<MainTabParamList>;
  // 모달 스크린
  //TeamSelectModal: undefined;
};

// 인증 스택 파라미터
export type AuthStackParamList = {
  userInfo: any;
  Landing: undefined;
  Login: undefined;
  TeamSelect: {
    nickname: string;
  };
  SignUp: undefined;
};

// 메인 탭 파라미터
export type MainTabParamList = {
  HomeStack: NavigatorScreenParams<HomeStackParamList>;
  ExploreStack: NavigatorScreenParams<ExploreStackParamList>;
  ChatStack: NavigatorScreenParams<ChatStackParamList>;
  MyPageStack: NavigatorScreenParams<MyPageStackParamList>;
};

// 홈 스택 파라미터
export type HomeStackParamList = {
  Home: undefined;
  PostList: {
    teamId?: string;
  };
  PostDetail: {
    postId: string;
  };
  PostCreate: {
    category?: string;
  };
  Notifications: undefined;
  Search: {
    initialQuery?: string;
  };
};

// 탐색 스택 파라미터
export type ExploreStackParamList = {
  CommunityHome: {
    teamId?: string;
  };
  PostEdit: {
    postId: string;
  };
};

// 채팅 스택 파라미터
export type ChatStackParamList = {
  ChatList: undefined;
  ChatRoom: {
    roomId: string;
    roomType: 'match';
    roomName?: string;
  };
  MatchingCreate: {
    gameId?: string;
  };
  MatchingDetail: {
    matchingId: string;
  };
};

// 마이페이지 스택 파라미터
export type MyPageStackParamList = {
  MyPage: undefined;
  Profile: {
    userId: string;
  };
  ProfileEdit: undefined;
  AttendanceHistory: undefined;
  Settings: undefined;
  NotificationSettings: undefined;
  BlockedUsers: undefined;
  About: undefined;
};

// 스크린 Props 타입
export type RootStackScreenProps<T extends keyof RootStackParamList> = StackScreenProps<RootStackParamList, T>;

export type AuthStackScreenProps<T extends keyof AuthStackParamList> = CompositeScreenProps<
  StackScreenProps<AuthStackParamList, T>,
  RootStackScreenProps<'AuthStack'>
>;

export type MainTabScreenProps<T extends keyof MainTabParamList> = CompositeScreenProps<
  BottomTabScreenProps<MainTabParamList, T>,
  RootStackScreenProps<'MainTabs'>
>;

export type HomeStackScreenProps<T extends keyof HomeStackParamList> = CompositeScreenProps<
  StackScreenProps<HomeStackParamList, T>,
  MainTabScreenProps<'HomeStack'>
>;

export type ExploreStackScreenProps<T extends keyof ExploreStackParamList> = CompositeScreenProps<
  StackScreenProps<ExploreStackParamList, T>,
  MainTabScreenProps<'ExploreStack'>
>;

export type ChatStackScreenProps<T extends keyof ChatStackParamList> = CompositeScreenProps<
  StackScreenProps<ChatStackParamList, T>,
  MainTabScreenProps<'ChatStack'>
>;

export type MyPageStackScreenProps<T extends keyof MyPageStackParamList> = CompositeScreenProps<
  StackScreenProps<MyPageStackParamList, T>,
  MainTabScreenProps<'MyPageStack'>
>;

// 네비게이션 헬퍼 타입
export type NavigationRoute<T extends Record<string, any>> = {
  key: string;
  name: keyof T;
  params?: T[keyof T];
};

// 딥링크 설정 타입
export type DeepLinkConfig = {
  [key: string]: {
    path: string;
    params?: Record<string, string>;
    parse?: Record<string, (value: string) => any>;
  };
};

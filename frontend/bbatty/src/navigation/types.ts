import { NavigatorScreenParams } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps } from '@react-navigation/native';

// 루트 스택 파라미터
export type RootStackParamList = {
  AuthStack: NavigatorScreenParams<AuthStackParamList>;
  MainTabs: NavigatorScreenParams<MainTabParamList>;
};

// 인증 스택 파라미터
export type AuthStackParamList = {
  TeamSelect: undefined;
  SignUp: {
    teamId: number;
    onSignUpComplete?: () => void;
  };
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
  PostForm: undefined;
  PostList: {
    teamId: number;
  };
  PostDetail: {
    postId: number;
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
  MatchChatRoomList: undefined;
  CreateMatchChatRoom: undefined;
  MatchChatRoomDetail: {
    room: {
      matchId: string;
      gameId: string | null;
      matchTitle: string;
      matchDescription: string;
      teamId: string;
      minAge: number;
      maxAge: number;
      genderCondition: 'ALL' | 'MALE' | 'FEMALE';
      maxParticipants: number;
      currentParticipants: number;
      createdAt: string;
      status: 'ACTIVE' | 'INACTIVE';
      websocketUrl: string;
    };
  };
  MatchChatRoom: {
    room: {
      matchId: string;
      gameId: string | null;
      matchTitle: string;
      matchDescription: string;
      teamId: string;
      minAge: number;
      maxAge: number;
      genderCondition: 'ALL' | 'MALE' | 'FEMALE';
      maxParticipants: number;
      currentParticipants: number;
      createdAt: string;
      status: 'ACTIVE' | 'INACTIVE';
      websocketUrl: string;
    };
    websocketUrl: string;
    sessionToken: string;
  };
};

// 마이페이지 스택 파라미터 (간소화)
export type MyPageStackParamList = {
  Profile: {
    userId?: number; // undefined면 본인 프로필
  };
  ProfileEdit: undefined;
  Settings: undefined;
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

import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { 
  MatchChatRoomListScreen, 
  CreateMatchChatRoomScreen,
  MatchChatRoomDetailScreen,
  MatchChatRoomScreen 
} from '../../pages/match-chat';
import ChatRoomSearchScreen from '../../pages/match-chat/ChatRoomSearchScreen';
import type { ChatStackParamList } from '../types';

const Stack = createStackNavigator<ChatStackParamList>();

export default function ChatNavigator() {
  return (
    <Stack.Navigator
      initialRouteName="MatchChatRoomList"
      screenOptions={{
        headerStyle: {
          backgroundColor: '#fff',
        },
        headerTintColor: '#333',
        headerTitleStyle: {
          fontWeight: 'bold',
        },
        // 화면 캐싱 비활성화로 중복 마운트 방지
        animationEnabled: true,
        gestureEnabled: true,
      }}
    >
      <Stack.Screen 
        name="MatchChatRoomList" 
        component={MatchChatRoomListScreen}
        options={{ headerShown: false }}
      />
      <Stack.Screen 
        name="CreateMatchChatRoom" 
        component={CreateMatchChatRoomScreen}
        options={{ headerShown: false }}
      />
      <Stack.Screen 
        name="MatchChatRoomDetail" 
        component={MatchChatRoomDetailScreen}
        options={{ headerShown: false }}
      />
      <Stack.Screen 
        name="MatchChatRoom" 
        component={MatchChatRoomScreen}
        options={{ headerShown: false }}
      />
      <Stack.Screen 
        name="ChatRoomSearch" 
        component={ChatRoomSearchScreen}
        options={{ headerShown: false }}
      />
    </Stack.Navigator>
  );
}

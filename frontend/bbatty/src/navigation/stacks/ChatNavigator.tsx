import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { 
  MatchChatRoomListScreen, 
  CreateMatchChatRoomScreen,
  MatchChatRoomDetailScreen,
  MatchChatRoomScreen 
} from '../../pages/match-chat';
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
    </Stack.Navigator>
  );
}

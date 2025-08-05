import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { MainTabParamList } from './types';
import { useTheme } from '../shared/styles';
import HomeNavigator from './stacks/HomeNavgator';
import ChatNavigator from './stacks/ChatNavigator';
import ExploreNavigator from './stacks/ExploreNavigator';
import MyPageNavigator from './stacks/MyPageNavigator';

const BottomTab = createBottomTabNavigator<MainTabParamList>();

export default function MainNavigator() {
  const { theme } = useTheme();

  return (
    <BottomTab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: theme.colors.primary,
        tabBarInactiveTintColor: theme.colors.text.secondary,
        tabBarStyle: {
          backgroundColor: theme.colors.surface,
          borderTopColor: theme.colors.border,
        },
      }}
    >
      <BottomTab.Screen name='HomeStack' component={HomeNavigator} options={{ tabBarLabel: '홈' }} />
      <BottomTab.Screen name='ExploreStack' component={ExploreNavigator} options={{ tabBarLabel: '탐색' }} />
      <BottomTab.Screen name='ChatStack' component={ChatNavigator} options={{ tabBarLabel: '채팅' }} />
      <BottomTab.Screen name='MyPageStack' component={MyPageNavigator} options={{ tabBarLabel: 'My' }} />
    </BottomTab.Navigator>
  );
}

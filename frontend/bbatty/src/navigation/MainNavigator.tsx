import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
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
      <BottomTab.Screen 
        name='HomeStack' 
        component={HomeNavigator} 
        options={{ 
          tabBarLabel: '홈',
          tabBarIcon: ({ focused, color, size }) => (
            <Ionicons 
              name={focused ? 'home' : 'home-outline'} 
              size={size} 
              color={color} 
            />
          ),
        }} 
      />
      <BottomTab.Screen 
        name='ExploreStack' 
        component={ExploreNavigator} 
        options={{ 
          tabBarLabel: '탐색',
          tabBarIcon: ({ focused, color, size }) => (
            <Ionicons 
              name={focused ? 'search' : 'search-outline'} 
              size={size} 
              color={color} 
            />
          ),
        }} 
      />
      <BottomTab.Screen 
        name='ChatStack' 
        component={ChatNavigator} 
        options={{ 
          tabBarLabel: '채팅',
          tabBarIcon: ({ focused, color, size }) => (
            <Ionicons 
              name={focused ? 'chatbubbles' : 'chatbubbles-outline'} 
              size={size} 
              color={color} 
            />
          ),
        }} 
      />
      <BottomTab.Screen 
        name='MyPageStack' 
        component={MyPageNavigator} 
        options={{ 
          tabBarLabel: 'My',
          tabBarIcon: ({ focused, color, size }) => (
            <Ionicons 
              name={focused ? 'person' : 'person-outline'} 
              size={size} 
              color={color} 
            />
          ),
        }} 
      />
    </BottomTab.Navigator>
  );
}

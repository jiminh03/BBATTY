import React, { useState } from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { getFocusedRouteNameFromRoute } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { MainTabParamList } from './types';
import { useThemeColor } from '../shared/team/ThemeContext';
import { useTabBar } from '../shared/contexts/TabBarContext';
import HomeNavigator from './stacks/HomeNavigator';
import ChatNavigator from './stacks/ChatNavigator';
import ExploreNavigator from './stacks/ExploreNavigator';
import MyPageNavigator from './stacks/MyPageNavigator';
import { CommonActions } from '@react-navigation/native';

// 탭바를 숨길 화면들 정의
const getTabBarVisibility = (route: any) => {
  const routeName = getFocusedRouteNameFromRoute(route) ?? 'MatchChatRoomList';


  if (routeName === 'MatchChatRoom') {
    return { display: 'none' };
  }
  return { display: 'flex' };
};


const BottomTab = createBottomTabNavigator<MainTabParamList>();

export default function MainNavigator() {
  const themeColor = useThemeColor();
  const { isTabBarVisible } = useTabBar();

  return (
    <BottomTab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: themeColor,
        tabBarStyle: isTabBarVisible ? {
          borderTopColor: themeColor,
        } : {
          display: 'none'
        },
      }}
      initialRouteName='HomeStack'
    >
      <BottomTab.Screen
        name='HomeStack'
        component={HomeNavigator}
        options={{
          tabBarLabel: '홈',
          tabBarIcon: ({ focused, color, size }) => (
            <Ionicons name={focused ? 'home' : 'home-outline'} size={size} color={color} />
          ),
        }}
      />
      <BottomTab.Screen
        name='ExploreStack'
        component={ExploreNavigator}
        options={{
          tabBarLabel: '탐색',
          tabBarIcon: ({ focused, color, size }) => (
            <Ionicons name={focused ? 'search' : 'search-outline'} size={size} color={color} />
          ),
        }}
      />
      <BottomTab.Screen
        name='ChatStack'
        component={ChatNavigator}
        options={({ route }) => ({
          tabBarLabel: '채팅',
          tabBarIcon: ({ focused, color, size }) => (
            <Ionicons name={focused ? 'chatbubbles' : 'chatbubbles-outline'} size={size} color={color} />
          ),
          tabBarStyle: getTabBarVisibility(route),
        })}
      />
      <BottomTab.Screen
        name='MyPageStack'
        component={MyPageNavigator}
        options={{
          tabBarLabel: 'My',
          tabBarIcon: ({ focused, color, size }) => (
            <Ionicons name={focused ? 'person' : 'person-outline'} size={size} color={color} />
          ),
        }}
        listeners={({ navigation }) => ({
          tabPress: (e) => {
            // 기본 동작 방지
            e.preventDefault();
            
            // MyPageStack을 Profile 화면으로 완전 리셋
            navigation.dispatch(
              CommonActions.reset({
                index: 0,
                routes: [
                  {
                    name: 'MyPageStack',
                    state: {
                      routes: [{ name: 'Profile' }],
                      index: 0,
                    }
                  }
                ]
              })
            );
          },
        })}
      />
    </BottomTab.Navigator>
  );
}

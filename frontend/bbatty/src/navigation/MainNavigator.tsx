import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { getFocusedRouteNameFromRoute } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { MainTabParamList } from './types';
import { useThemeColor } from '../shared/team/ThemeContext';
import HomeNavigator from './stacks/HomeNavigator';
import ChatNavigator from './stacks/ChatNavigator';
import ExploreNavigator from './stacks/ExploreNavigator';
import MyPageNavigator from './stacks/MyPageNavigator';

// 탭바를 숨길 화면들 정의
const getTabBarVisibility = (route: any) => {
  const routeName = getFocusedRouteNameFromRoute(route) ?? 'MatchChatRoomList';

  console.log('현재 화면:', routeName); // 디버깅용

  if (routeName === 'MatchChatRoom') {
    return { display: 'none' };
  }
  return { display: 'flex' };
};

const BottomTab = createBottomTabNavigator<MainTabParamList>();

export default function MainNavigator() {
  const themeColor = useThemeColor();

  return (
    <BottomTab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: themeColor,
        tabBarStyle: {
          borderTopColor: themeColor,
        },
      }}
      initialRouteName='HomeStack'
      screenListeners={{
        tabPress: (e) => {
          // 탭 바 눌렀을 때 로그 출력
          console.log('Tab pressed:', e.target);
        },
      }}
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
      />
    </BottomTab.Navigator>
  );
}

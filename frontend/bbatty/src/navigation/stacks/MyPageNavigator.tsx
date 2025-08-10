import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { MyPageStackParamList } from '../types';

// Pages
import UserProfileScreen from '../../pages/myPage/ui/UserProfileScreen';
import EditProfileScreen from '../../pages/myPage/ui/EditProfileScreen';
import SettingsScreen from '../../pages/myPage/ui/SettingsScreen';

const Stack = createStackNavigator<MyPageStackParamList>();

export default function MyPageNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
        gestureEnabled: true,
      }}
      initialRouteName='Profile'
    >
      <Stack.Screen
        name='Profile'
        component={UserProfileScreen}
        initialParams={{}} // 본인 프로필로 시작
      />
      <Stack.Screen name='ProfileEdit' component={EditProfileScreen} />
      <Stack.Screen name='Settings' component={SettingsScreen} />
    </Stack.Navigator>
  );
}

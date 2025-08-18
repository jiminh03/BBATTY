import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { MyPageStackParamList } from '../types';

// Pages
import UserProfileScreen from '../../pages/myPage/ui/UserProfileScreen';
import OtherUserProfileScreen from '../../pages/myPage/ui/OtherUserProfileScreen';
import EditProfileScreen from '../../pages/myPage/ui/EditProfileScreen';
import SettingsScreen from '../../pages/myPage/ui/SettingsScreen';
import PostDetailScreen from '../../pages/home/PostDetailScreen';

const Stack = createStackNavigator<MyPageStackParamList>();

// MyPageNavigator.tsx
export default function MyPageNavigator() {
  return (
    <Stack.Navigator 
      screenOptions={{ headerShown: false }}
      initialRouteName='Profile'
    >
      <Stack.Screen name='Profile' component={UserProfileScreen} />
      <Stack.Screen name='OtherProfile' component={OtherUserProfileScreen} />
      <Stack.Screen name='ProfileEdit' component={EditProfileScreen} />
      <Stack.Screen name='Settings' component={SettingsScreen} />
      <Stack.Screen name='PostDetail' component={PostDetailScreen}/>
    </Stack.Navigator>
  );
}

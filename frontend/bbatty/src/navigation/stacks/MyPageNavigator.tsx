import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { MyPageStackParamList } from '../types';
import MyPageScreen from '../../pages/myPage';

const Stack = createStackNavigator<MyPageStackParamList>();

export default function MyPageNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen name="MyPage" component={MyPageScreen} />
    </Stack.Navigator>
  );
}

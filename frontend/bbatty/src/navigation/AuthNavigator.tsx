import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { AuthStackParamList } from './types';
import LandingScreen from '../pages/landing';
import LoginScreen from '../pages/login';
import SignUpScreen from '../pages/signup';
import TeamSelectScreen from '../pages/team-select';

const Stack = createStackNavigator<AuthStackParamList>();

export default function AuthNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: '#fff' },
        headerTintColor: '#000',
        headerTitleStyle: { fontWeight: 'bold' },
      }}
    >
      <Stack.Screen name='Landing' component={LandingScreen} options={{ headerShown: false }} />
      <Stack.Screen name='Login' component={LoginScreen} options={{ title: '로그인' }} />
      <Stack.Screen name='SignUp' component={SignUpScreen} options={{ title: '회원가입' }} />
      <Stack.Screen name='TeamSelect' component={TeamSelectScreen} options={{ title: '팀 선택' }} />
    </Stack.Navigator>
  );
}

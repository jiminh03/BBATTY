import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { AuthStackParamList } from './types';
import SignUpScreen from '../pages/signup';
import TeamSelectScreen from '../pages/teamSelect/ui/TeamSelectScreen';

const Stack = createStackNavigator<AuthStackParamList>();
interface AuthNavigatorProps {
  userInfo: any;
}

export default function AuthNavigator({ userInfo }: AuthNavigatorProps) {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: '#fff' },
        headerTintColor: '#000',
        headerTitleStyle: { fontWeight: 'bold' },
      }}
    >
      <Stack.Screen
        name='TeamSelect'
        component={TeamSelectScreen}
        initialParams={{ nickname: userInfo?.kakao_account?.email }}
        options={{ title: '팀 선택' }}
      />
      <Stack.Screen name='SignUp' component={SignUpScreen} options={{ title: '회원가입' }} />
    </Stack.Navigator>
  );
}

import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { AuthStackParamList } from './types';
import SignUpScreen from '../pages/signup';
import TeamSelectScreen from '../pages/teamSelect/ui/TeamSelectScreen';

const Stack = createStackNavigator<AuthStackParamList>();

export default function AuthNavigator() {
  return (
    <Stack.Navigator
      initialRouteName='TeamSelect'
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen name='TeamSelect' component={TeamSelectScreen} />
      <Stack.Screen name='SignUp' component={SignUpScreen} />
    </Stack.Navigator>
  );
}

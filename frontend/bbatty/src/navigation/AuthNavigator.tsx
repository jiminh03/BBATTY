import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { AuthStackParamList } from './types';
import SignUpScreen from '../pages/signup';
import TeamSelectScreen from '../pages/teamSelect/ui/TeamSelectScreen';

const Stack = createStackNavigator<AuthStackParamList>();

interface AuthNavigatorProps {
  onSignUpComplete?: () => void;
}

export default function AuthNavigator({ onSignUpComplete }: AuthNavigatorProps) {
  return (
    <Stack.Navigator
      initialRouteName='TeamSelect'
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen name='TeamSelect' component={TeamSelectScreen} />
      <Stack.Screen name='SignUp'>
        {(props) => <SignUpScreen {...props} onSignUpComplete={onSignUpComplete} />}
      </Stack.Screen>
    </Stack.Navigator>
  );
}

import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { AuthStackParamList } from './types';
import SignUpScreen from '../pages/signup';
import TeamSelectScreen from '../pages/teamSelect/ui/TeamSelectScreen';
import { Token } from '../shared/api/token/tokenTypes';

const Stack = createStackNavigator<AuthStackParamList>();

interface AuthNavigatorProps {
  onSignUpComplete?: (userInfo: any, tokens: Token) => void;
  isExistingUser?: boolean;
}

export default function AuthNavigator({ onSignUpComplete, isExistingUser = false }: AuthNavigatorProps) {
  const initialRouteName = isExistingUser ? 'SignUp' : 'TeamSelect';

  return (
    <Stack.Navigator
      initialRouteName={initialRouteName}
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen name='TeamSelect' component={TeamSelectScreen} />
      <Stack.Screen name='SignUp'>
        {(props) => <SignUpScreen {...props} onSignUpComplete={onSignUpComplete} isExistingUser={isExistingUser} />}
      </Stack.Screen>
    </Stack.Navigator>
  );
}

import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { ExploreStackParamList } from '../types';
import { CommunityHomeScreen } from '../../pages/explore';

const Stack = createStackNavigator<ExploreStackParamList>();

export default function ExploreNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen 
        name="CommunityHome" 
        component={CommunityHomeScreen}
        initialParams={{}}
      />
    </Stack.Navigator>
  );
}

import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { ExploreStackParamList } from '../types';
import { CommunityHomeScreen } from '../../pages/explore';
import TeamRankingScreen from '../../pages/explore/TeamRankingScreen';
import UserRankingScreen from '../../pages/explore/UserRankingScreen';
import TeamCommunityScreen from '../../pages/explore/TeamCommunityScreen';
import PostDetailScreen from '../../pages/home/PostDetailScreen';


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
      />
      <Stack.Screen 
        name="TeamRanking" 
        component={TeamRankingScreen}
      />
      <Stack.Screen 
        name="UserRanking" 
        component={UserRankingScreen}
      />
      <Stack.Screen 
        name="TeamCommunity" 
        component={TeamCommunityScreen}
      />
      <Stack.Screen
        name="PostDetail"
        component={PostDetailScreen}/>
    </Stack.Navigator>
  );
}

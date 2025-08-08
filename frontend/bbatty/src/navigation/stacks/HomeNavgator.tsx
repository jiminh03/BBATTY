import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { HomeStackParamList } from '../types';
import HomeScreen from '../../pages/home';
import { PostForm } from '../../entities/post/ui/PostForm';

const Stack = createStackNavigator<HomeStackParamList>();

export default function HomeNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen name='Home' component={HomeScreen}></Stack.Screen>
      <Stack.Screen name="PostForm" component={PostForm} options={{ title: '게시글 작성' }} />
    </Stack.Navigator>
  );
}

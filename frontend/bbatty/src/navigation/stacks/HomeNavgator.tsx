import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { HomeStackParamList } from '../types';
import HomeScreen from '../../pages/home';
import { PostForm } from '../../entities/post/ui/PostForm';
import { PostListScreen } from '../../pages/home/PostListScreen';
import PostDetailScreen from '../../pages/home/PostDetailScreen';

const Stack = createStackNavigator<HomeStackParamList>();

export default function HomeNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen name='Home' component={HomeScreen}></Stack.Screen>
      <Stack.Screen name="PostForm" component={PostForm} options={{ title: '게시글 작성' }} />
      <Stack.Screen name="PostList" component={PostListScreen} options={{ title: '게시글 목록' }} />
      <Stack.Screen name="PostDetail" component={PostDetailScreen} options={{ title: '게시글' }} />
    </Stack.Navigator>
  );
}

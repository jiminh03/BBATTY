import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { NavigationContainer } from '@react-navigation/native';
import { PostForm } from '../entities/post/ui/PostForm'; // 실제 경로로 수정 필요!

const Stack = createStackNavigator();

export default function AppNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen name="WritePost" component={PostForm} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

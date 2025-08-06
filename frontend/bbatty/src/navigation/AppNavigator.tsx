import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { StatusBar } from 'react-native';
import { useTheme } from '../shared/styles';
import { tokenManager } from '../shared';
import { RootStackParamList } from './types';
import AuthNavigator from './AuthNavigator';
import MainNavigator from './MainNavigator';
// import TeamSelectModal from '@/screens/modals/TeamSelectModal';
// import ImageViewerModal from '@/screens/modals/ImageViewerModal';
// import ReportModal from '@/screens/modals/ReportModal';
// import SplashScreen from '@/widgets/SplashScreen';
// import { SplashS }
import { navigationRef } from './naviagtionRefs';
import { linking } from './linking';
import SplashScreen from '../pages/SplashScreen';
import { NavigationContainer } from '@react-navigation/native';
import { PostForm } from '../entities/post/ui/PostForm'; // 실제 경로로 수정 필요!

const Stack = createStackNavigator();

export default function AppNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen name='WritePost' component={PostForm} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

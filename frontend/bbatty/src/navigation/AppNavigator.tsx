// 예시: app/appNavigator.tsx 또는 app/navigation/index.tsx 등
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { PostListScreen } from '../pages/home/PostListScreen'; // 네 경로에 맞게 조정

const Stack = createNativeStackNavigator();

export const AppNavigator = () => {
  return (
    <Stack.Navigator initialRouteName="PostList">
      <Stack.Screen name="PostList" component={PostListScreen} />
    </Stack.Navigator>
  );
};

// 예시: app/appNavigator.tsx 또는 app/navigation/index.tsx 등
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { PostListScreen } from '../pages/home/PostListScreen'; // 네 경로에 맞게 조정

const Stack = createStackNavigator();

export default function AppNavigator() {
  const [userInfo, setUserInfo] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showSplash, setShowSplash] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const { theme } = useTheme();

  useEffect(() => {
    checkAuthState();
  }, []);

  const checkAuthState = async () => {
    try {
      const token = await tokenManager.getToken();
      // 개발 중에는 항상 인증된 상태로 설정 (테스트 목적)
      setIsAuthenticated(true);
      // setIsAuthenticated(!!token);
    } catch (error) {
      console.error('Auth check error ', error);
      setIsAuthenticated(true); // 개발 중 우회
      // setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLoginSuccess = (userInfo: any) => {
    setIsAuthenticated(true);
    setShowSplash(false);
    setUserInfo(userInfo);

    // //로그인 성공 시 팀 선택 화면으로 이동
    // navigationRef.navigate('AuthStack', {
    //   screen: 'TeamSelect',
    //   params: {
    //     nickname: userInfo.properties?.nickname,
    //   },
    // });
  };

  if (showSplash) {
    return (
      <SplashScreen
        onAnimationComplete={() => {
          setShowSplash(false);
        }}
        onLoginSuccess={handleLoginSuccess}
      ></SplashScreen>
    );
  }

export const AppNavigator = () => {
  return (
    <NavigationContainer ref={navigationRef} linking={linking}>
      {/* <StatusBar barStyle='light-content' backgroundColor={theme.colors.background} /> */}
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {isAuthenticated ? (
          <Stack.Screen name='MainTabs' component={MainNavigator} />
        ) : (
          <Stack.Screen name='AuthStack' children={() => <AuthNavigator userInfo={userInfo} />} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
};

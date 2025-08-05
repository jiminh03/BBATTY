import React from 'react';
import { View, TouchableOpacity, Text } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';

type Props = HomeStackScreenProps<'Home'>;

export default function HomeScreen({ navigation, route }: Props) {
  /*
  // route.params로 전달받은 파라미터에 접근
  const { date, teamId } = route.params || {};

  const handleGamePress = (gameId: string) => {
    // TypeScript가 gameId가 필요하다고 알려줍니다
    navigation.navigate('GameDetail', { gameId });
  };

  const handleAttendancePress = (gameId: string, stadiumId: string) => {
    // 모든 필수 파라미터를 전달해야 합니다
    navigation.navigate('AttendanceVerify', { 
      gameId, 
      stadiumId 
    });
  };

  // 다른 스택으로 이동할 때
  const goToProfile = (userId: string) => {
    // 중첩된 네비게이션 구조를 타입 안전하게 처리
    navigation.navigate('MyPageStack', {
      screen: 'Profile',
      params: { userId }
    });
  };

  return (
    <View>
      { 화면 내용...}
    </View>
  );
}
  */
  return <></>;
}

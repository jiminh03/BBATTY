import { use, useState } from 'react';
import { Alert } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { extractData, tokenManager } from '../../../shared';
import { useUserStore } from '../../user';
import { authApi } from '../api/authApi';
import { navigationRef } from '../../../navigation/navigationRefs';

export const useKakaoLogin = () => {
  const navigation = useNavigation();
  const setCurrentUser = useUserStore((state) => state.setCurrentUser);
  const [isLoading, setIsLoading] = useState(false);

  const handleKakaoLogin = async (kakaoAccessToken: string) => {
    setIsLoading(true);

    try {
      // 서버에 카카오 토큰 전송
      console.log('WAS에 전송');
      const response = await authApi.kakaoLogin(kakaoAccessToken);
      console.log(`response : ${response} `);
      const data = extractData(response.data);
      console.log(`data : ${data} `);
      if (!data) return;

      // 액세스 토큰 저장
      await tokenManager.setToken(data.token.accessToken);

      const { user } = data;
      // 사용자 정보 저장
      setCurrentUser({
        id: user.id,
        nickname: user.nickname,
        profileImageURL: user.profileImageURL,
        teamId: user.teamId,
        gender: user.gender,
        age: user.age,
        updateAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      });

      console.log('성공');
      console.log(user);
      navigationRef.navigate('AuthStack', {
        screen: 'TeamSelect',
        params: {
          nickname: user.nickname,
        },
      });

      /*
      // 팀 선택 여부에 따라 화면 이동
      if (data.teamId) {
        // 이미 팀이 선택되어 있으면 메인으로
        navigation.reset({
          index: 0,
          routes: [{ name: 'MainTabs' as any }],
        });
      } else {
        // 팀 선택이 안되어 있으면 팀 선택 화면으로
      }*/
    } catch (error) {
      console.error('카카오 로그인 실패:', error);
      Alert.alert('로그인 실패', '다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  return {
    handleKakaoLogin,
    isLoading,
  };
};

// 팀 ID로 팀 이름 가져오기 (임시)
const getTeamNameById = (teamId: number): any => {
  const teamMap: Record<number, any> = {
    1: 'HANWHA',
    2: 'SAMSUNG',
    3: 'DOOSAN',
    4: 'NC',
    5: 'LG',
    6: 'KIA',
    7: 'KT',
    8: 'SSG',
    9: 'KIWOOM',
    10: 'LOTTE',
  };
  return teamMap[teamId] || null;
};

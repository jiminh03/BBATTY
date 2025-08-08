import React, { useState } from 'react';
import { View, ScrollView, KeyboardAvoidingView, Platform, Alert, TouchableOpacity, Text } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { AuthStackScreenProps } from '../../../navigation/types';
import { authApi } from '../../../entities/auth/api/authApi';
import { ProfileForm, NicknameConflictModal } from '../../../features/user-profile';
import { styles } from './SignUpScreen.style';
import { extractData, tokenManager } from '../../../shared';
import { useAuthStore } from '../../../entities/auth/model/authStore';
import { ProfileFormData } from '../../../features/user-profile/model/profileTypes';
import { RegisterRequest } from '../../../entities/auth';

type Props = AuthStackScreenProps<'SignUp'>;

export default function SignUpScreen({ route }: Props) {
  const navigation = useNavigation<AuthStackScreenProps<'SignUp'>['navigation']>();
  const insets = useSafeAreaInsets();
  const [showConflictModal, setShowConflictModal] = useState(false);

  const teamId = route.params?.teamId;
  const { kakaoUserInfo, kakaoAccessToken } = useAuthStore();

  // 회원가입 완료
  const handleSubmit = async (data: ProfileFormData) => {
    if (!teamId) {
      Alert.alert('오류', '팀이 선택되지 않았습니다');
      return;
    }

    if (!kakaoUserInfo || !kakaoAccessToken) {
      Alert.alert('오류', '카카오 로그인 정보가 없습니다');
      return;
    }
    try {
      const registerData: RegisterRequest = {
        accessToken: kakaoAccessToken,
        kakaoId: kakaoUserInfo.id.toString(),
        email: kakaoUserInfo.kakao_account?.email,
        birthYear: kakaoUserInfo.kakao_account?.birthyear,
        gender: kakaoUserInfo.kakao_account?.gender,
        teamId,
        nickname: data.nickname,
        profileImg: data.profileImage || '',
        introduction: data.introduction || '',
      };

      const response = await authApi.signup(registerData);
        console.log(JSON.stringify(response));
        
        if(response.status !== 'SUCCESS') return;
        

      // 토큰들 저장
      await Promise.all([
        tokenManager.setToken(response.data.tokens.accessToken),
        tokenManager.setRefreshToken(response.data.tokens.refreshToken),
      ]);

      Alert.alert('성공', '회원가입이 완료되었습니다');

      // 메인 화면으로 이동
      navigation.reset({
        index: 0,
        routes: [{ name: 'MainTabs' as any }],
      });
    } catch (error: any) {
      console.log(error);
      // // 닉네임 중복 에러 처리 <= 이부분수정해야함
      // if (error.response?.data?.error?.code === 'NICKNAME_CONFLICT') {
      //   setShowConflictModal(true);
      // } else {
      //   Alert.alert('오류', '회원가입에 실패했습니다');
      // }
      throw error; // ProfileForm에서 로딩 상태 해제를 위해
    }
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScrollView
        contentContainerStyle={[
          styles.scrollContent,
          { paddingTop: insets.top + 20, paddingBottom: insets.bottom + 20 },
        ]}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps='handled'
      >
        <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
          <Text style={styles.backButtonText}>{'<'} 회원가입</Text>
        </TouchableOpacity>

        <ProfileForm onSubmit={handleSubmit} showNicknameField={true} isEditMode={false} />
      </ScrollView>

      <NicknameConflictModal visible={showConflictModal} onConfirm={() => setShowConflictModal(false)} />
    </KeyboardAvoidingView>
  );
}

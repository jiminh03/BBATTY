import React, { useEffect, useState } from 'react';
import { View, ScrollView, KeyboardAvoidingView, Platform, Alert, TouchableOpacity, Text } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { RouteProp, useNavigation } from '@react-navigation/native';
import { AuthStackParamList, AuthStackScreenProps } from '../../../navigation/types';
import { authApi } from '../../../features/user-auth';
import { ProfileForm, NicknameConflictModal } from '../../../features/user-profile';
import { styles } from './SignUpScreen.style';
import { usekakaoStore } from '../../../features/user-auth/model/kakaoStore';
import { ProfileFormData } from '../../../features/user-profile/model/profileTypes';
import { StackNavigationProp } from '@react-navigation/stack';
import { useUserStore } from '../../../entities/user';
import { useTokenStore } from '../../../shared/api/token/tokenStore';
import { isOk } from '../../../shared/utils/result';
import { Token } from '../../../shared/api/token/tokenTypes';

type SignUpScreenNavigationProp = StackNavigationProp<AuthStackParamList, 'SignUp'>;
type SignUpScreenRouteProp = RouteProp<AuthStackParamList, 'SignUp'>;

interface SignUpScreenProps {
  navigation: SignUpScreenNavigationProp;
  route: SignUpScreenRouteProp;
  onSignUpComplete?: (userInfo: any, tokens: Token) => void;
  isExistingUser?: boolean;
}

export default function SignUpScreen({
  navigation,
  route,
  onSignUpComplete,
  isExistingUser = false,
}: SignUpScreenProps) {
  const insets = useSafeAreaInsets();
  const [isLoading, setIsLoading] = useState(false);
  const [showConflictModal, setShowConflictModal] = useState(false);
  const { kakaoUserInfo, kakaoAccessToken } = usekakaoStore();

  const teamId = route.params?.teamId;
  useEffect(() => {
    if (isExistingUser && kakaoUserInfo && kakaoAccessToken) {
      handleExistingUserLogin();
    }
  }, [isExistingUser, kakaoUserInfo, kakaoAccessToken]);

  // 기존 회원 로그인
  const handleExistingUserLogin = async () => {
    if (!kakaoUserInfo || !kakaoAccessToken) {
      Alert.alert('오류', '카카오 로그인 정보가 없습니다.');
      return;
    }

    setIsLoading(true);
    const result = await authApi.login({
      accessToken: kakaoAccessToken,
    });

    setIsLoading(false);

    if (isOk(result)) {
      const { userProfile, tokens } = result.data;

      onSignUpComplete?.(userProfile, tokens);
    } else {
      console.error('Login failed:', result.error);
      Alert.alert('로그인 실패', result.error.message || '자동 로그인에 실패했습니다.');
    }
  };

  // 신규 회원가입
  const handleSignUp = async (formData: ProfileFormData) => {
    if (!teamId) {
      Alert.alert('오류', '팀이 선택되지 않았습니다.');
      return;
    }

    if (!kakaoUserInfo || !kakaoAccessToken) {
      Alert.alert('오류', '카카오 로그인 정보가 없습니다.');
      return;
    }

    const result = await authApi.signup({
      accessToken: kakaoAccessToken,
      kakaoId: kakaoUserInfo.id.toString(),
      nickname: formData.nickname,
      email: kakaoUserInfo.kakao_account?.email || '',
      gender: kakaoUserInfo.kakao_account?.gender,
      birthYear: kakaoUserInfo.kakao_account?.birthyear,
      profileImg: formData.profileImage,
      introduction: formData.introduction,
      teamId: teamId,
    });

    if (isOk(result)) {
      const { userProfile, tokens } = result.data;
      onSignUpComplete?.(userProfile, tokens);
    } else {
      console.error('Sign up failed:', result.error);

      // 닉네임 중복 에러 처리
      if (result.error.code === 'NICKNAME_CONFLICT') {
        setShowConflictModal(true);
      } else {
        Alert.alert('회원가입 실패', result.error.message || '회원가입에 실패했습니다.');
      }

      throw new Error(result.error.message); // ProfileForm에서 로딩 해제용
    }
  };

  if (isExistingUser && isLoading) {
    return (
      <View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
        <Text>로그인 중...</Text>
      </View>
    );
  }

  if (isExistingUser) {
    return null;
  }

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

        <ProfileForm onSubmit={handleSignUp} showNicknameField={true} isEditMode={false} />
      </ScrollView>

      <NicknameConflictModal visible={showConflictModal} onConfirm={() => setShowConflictModal(false)} />
    </KeyboardAvoidingView>
  );
}

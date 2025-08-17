import React, { useMemo } from 'react';
import { View, ScrollView, KeyboardAvoidingView, Platform, Alert, TouchableOpacity, Text } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';

import { MyPageStackParamList } from '../../../navigation/types';
import { ProfileForm, useProfile, useUpdateProfile } from '../../../features/user-profile';
import { ProfileFormData } from '../../../features/user-profile/model/profileTypes';
import { useUserStore } from '../../../entities/user';
import { styles } from './EditProfileScreen.style';

type EditProfileScreenNavigationProp = StackNavigationProp<MyPageStackParamList, 'ProfileEdit'>;

export default function EditProfileScreen() {
  const navigation = useNavigation<EditProfileScreenNavigationProp>();
  const insets = useSafeAreaInsets();
  const { currentUser } = useUserStore();

  const { data, isLoading } = useProfile(); // 본인 프로필 조회
  
  const updateProfileMutation = useUpdateProfile();

  const handleUpdateProfile = async (formData: ProfileFormData) => {
    updateProfileMutation.mutate(
      {
        nickname: formData.nickname,
        profileImg: formData.profileImage,
        introduction: formData.introduction,
      },
      {
        onSuccess: async (updatedProfile) => {
          // userStore 갱신 (모든 기존 필드 유지하면서 변경된 부분만 업데이트)
          const { setCurrentUser, getCurrentUser } = useUserStore.getState();
          const currentUser = getCurrentUser();
          if (currentUser && updatedProfile) {
            // 기존 사용자 정보를 모두 유지하면서 프로필에서 변경된 부분만 병합
            await setCurrentUser({
              ...currentUser, // teamId, userId, age, gender 등 모든 기존 필드 유지
              nickname: updatedProfile.nickname || currentUser.nickname,
              profileImg: updatedProfile.profileImg || currentUser.profileImg,
            });
          }
          
          Alert.alert('성공', '프로필이 업데이트되었습니다.', [{ text: '확인', onPress: () => navigation.goBack() }]);
        },
        onError: (error: Error) => {
          Alert.alert('실패', error.message || '프로필 업데이트에 실패했습니다.');
        },
      }
    );
  };

  // initialData 메모이제이션으로 불필요한 재렌더링 방지
  const initialFormData = useMemo(() => ({
    nickname: data?.nickname,
    profileImage: data?.profileImg,
    introduction: data?.introduction,
  }), [data?.nickname, data?.profileImg, data?.introduction]);

  if (isLoading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text>프로필을 불러오는 중...</Text>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScrollView
        style={[styles.container, { paddingTop: insets.top }]}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: insets.bottom + 20 }]}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps='handled'
      >
        <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
          <Text style={styles.backButtonText}>{'<'} 프로필 변경</Text>
        </TouchableOpacity>

        <ProfileForm
          initialData={initialFormData}
          onSubmit={handleUpdateProfile}
          showNicknameField={true}
          originalNickname={data?.nickname} // 기존 닉네임 전달
        />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

import React from 'react';
import { View, ScrollView, KeyboardAvoidingView, Platform, Alert, TouchableOpacity, Text } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { MyPageStackParamList } from '../../../navigation/types';
import { ProfileForm, profileApi, useProfile } from '../../../features/user-profile';
import { ProfileFormData } from '../../../features/user-profile/model/profileTypes';
import { useUserStore } from '../../../entities/user';
import { isOk } from '../../../shared/utils/result';
import { styles } from './EditProfileScreen.style';

type EditProfileScreenNavigationProp = StackNavigationProp<MyPageStackParamList, 'ProfileEdit'>;

export default function EditProfileScreen() {
  const navigation = useNavigation<EditProfileScreenNavigationProp>();
  const insets = useSafeAreaInsets();
  const { currentUser } = useUserStore();
  const queryClient = useQueryClient();

  const { profile, isLoading } = useProfile(); // 본인 프로필 조회

  const updateProfileMutation = useMutation({
    mutationFn: async (formData: ProfileFormData) => {
      const result = await profileApi.updateProfile({
        nickname: formData.nickname,
        profileImg: formData.profileImage,
        introduction: formData.introduction,
      });

      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      Alert.alert('성공', '프로필이 업데이트되었습니다.', [{ text: '확인', onPress: () => navigation.goBack() }]);
    },
    onError: (error: Error) => {
      Alert.alert('실패', error.message || '프로필 업데이트에 실패했습니다.');
    },
  });

  const handleUpdateProfile = async (formData: ProfileFormData) => {
    updateProfileMutation.mutate(formData);
  };

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
        contentContainerStyle={[
          styles.scrollContent,
          { paddingTop: insets.top + 20, paddingBottom: insets.bottom + 20 },
        ]}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps='handled'
      >
        <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
          <Text style={styles.backButtonText}>{'<'} 프로필 변경</Text>
        </TouchableOpacity>

        <ProfileForm
          initialData={{
            nickname: profile?.nickname,
            profileImage: profile?.profileImg,
            introduction: profile?.introduction,
          }}
          onSubmit={handleUpdateProfile}
          isEditMode={true}
          showNicknameField={false} // 편집 모드에서는 닉네임 변경 불가
        />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

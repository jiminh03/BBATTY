export default function SignUpScreen() {
  return <></>;
}

// import React, { useState } from 'react';
// import { View, ScrollView, KeyboardAvoidingView, Platform, Alert, TouchableOpacity, Text } from 'react-native';
// import { useSafeAreaInsets } from 'react-native-safe-area-context';
// import { useNavigation } from '@react-navigation/native';
// import { AuthStackScreenProps } from '@/shared/navigation/types';
// import { authApi } from '@/features/auth/api';
// import { ProfileForm, ProfileFormData } from '@/features/profile/ui/ProfileForm';
// import { NicknameConflictModal } from './ui/NicknameConflictModal';
// import { styles } from './styles';

// type Props = AuthStackScreenProps<'SignUpComplete'>;

// export default function SignUpScreen({ route }: Props) {
//   const navigation = useNavigation();
//   const insets = useSafeAreaInsets();
//   const [showConflictModal, setShowConflictModal] = useState(false);

//   // 닉네임 중복 확인
//   const handleCheckNickname = async (nickname: string): Promise<boolean> => {
//     try {
//       const response = await authApi.checkNickname(nickname);
//       return response.data.data.available;
//     } catch (error) {
//       Alert.alert('오류', '닉네임 확인에 실패했습니다');
//       return false;
//     }
//   };

//   // 회원가입 완료
//   const handleSubmit = async (data: ProfileFormData) => {
//     try {
//       await authApi.completeSignUp({
//         nickname: data.nickname,
//         profileImage: data.profileImage || undefined,
//         introduction: data.introduction || undefined,
//       });

//       // 메인 화면으로 이동
//       navigation.reset({
//         index: 0,
//         routes: [{ name: 'MainTabs' as any }],
//       });
//     } catch (error: any) {
//       // 닉네임 중복 에러 처리
//       if (error.response?.data?.error?.code === 'NICKNAME_CONFLICT') {
//         setShowConflictModal(true);
//       } else {
//         Alert.alert('오류', '회원가입에 실패했습니다');
//       }
//       throw error; // ProfileForm에서 로딩 상태 해제를 위해
//     }
//   };

//   return (
//     <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
//       <ScrollView
//         contentContainerStyle={[
//           styles.scrollContent,
//           { paddingTop: insets.top + 20, paddingBottom: insets.bottom + 20 },
//         ]}
//         showsVerticalScrollIndicator={false}
//         keyboardShouldPersistTaps='handled'
//       >
//         <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
//           <Text style={styles.backButtonText}>{'<'} 회원가입</Text>
//         </TouchableOpacity>

//         <ProfileForm
//           onSubmit={handleSubmit}
//           onCheckNickname={handleCheckNickname}
//           showNicknameField={true}
//           isEditMode={false}
//         />
//       </ScrollView>

//       <NicknameConflictModal visible={showConflictModal} onConfirm={() => setShowConflictModal(false)} />
//     </KeyboardAvoidingView>
//   );
// }

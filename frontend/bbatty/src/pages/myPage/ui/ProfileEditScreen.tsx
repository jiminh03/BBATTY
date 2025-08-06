// import React from 'react';
// import { View, ScrollView, KeyboardAvoidingView, Platform, Alert, TouchableOpacity, Text } from 'react-native';
// import { useSafeAreaInsets } from 'react-native-safe-area-context';
// import { useNavigation } from '@react-navigation/native';
// import { MyPageStackScreenProps } from '@/shared/navigation/types';
// import { useMe, useUpdateProfile } from '@/entities/user/queries';
// import { ProfileForm, ProfileFormData } from '@/features/profile/ui/ProfileForm';
// import { styles } from './styles';

// type Props = MyPageStackScreenProps<'ProfileEdit'>;

// export default function ProfileEditScreen({ navigation }: Props) {
//   const insets = useSafeAreaInsets();
//   const { data: currentUser } = useMe();
//   const updateProfile = useUpdateProfile();

//   // 프로필 업데이트
//   const handleSubmit = async (data: ProfileFormData) => {
//     try {
//       await updateProfile.mutateAsync({
//         bio: data.introduction,
//         profileImage: data.profileImage || undefined,
//       });

//       Alert.alert('성공', '프로필이 변경되었습니다', [{ text: '확인', onPress: () => navigation.goBack() }]);
//     } catch (error) {
//       Alert.alert('오류', '프로필 변경에 실패했습니다');
//       throw error;
//     }
//   };

//   if (!currentUser) {
//     return null;
//   }

//   return (
//     <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
//       <View style={[styles.header, { paddingTop: insets.top }]}>
//         <TouchableOpacity onPress={() => navigation.goBack()}>
//           <Text style={styles.headerButton}>{'<'}</Text>
//         </TouchableOpacity>
//         <Text style={styles.headerTitle}>프로필 변경</Text>
//         <View style={styles.headerButton} />
//       </View>

//       <ScrollView
//         contentContainerStyle={styles.scrollContent}
//         showsVerticalScrollIndicator={false}
//         keyboardShouldPersistTaps='handled'
//       >
//         <ProfileForm
//           initialData={{
//             nickname: currentUser.username,
//             profileImage: currentUser.profileImage,
//             introduction: currentUser.bio || '',
//           }}
//           onSubmit={handleSubmit}
//           showNicknameField={true}
//           isEditMode={true}
//         />
//       </ScrollView>
//     </KeyboardAvoidingView>
//   );
// }

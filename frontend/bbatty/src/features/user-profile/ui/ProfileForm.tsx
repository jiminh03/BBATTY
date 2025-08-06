// import React from 'react';
// import { View, Text, TextInput, TouchableOpacity, ActivityIndicator } from 'react-native';
// import { useTheme } from '@/shared/styles';
// import { ProfileImagePicker } from './ProfileImagePicker';
// import { useProfileForm } from '../hooks/useProfileForm';
// import { styles } from './styles';

// export interface ProfileFormData {
//   nickname: string;
//   profileImage: string | null;
//   introduction: string;
// }

// export interface ProfileFormProps {
//   initialData?: Partial<ProfileFormData>;
//   onSubmit: (data: ProfileFormData) => Promise<void>;
//   onCheckNickname?: (nickname: string) => Promise<boolean>;
//   isEditMode?: boolean;
//   showNicknameField?: boolean;
// }

// export const ProfileForm: React.FC<ProfileFormProps> = ({
//   initialData = {},
//   onSubmit,
//   onCheckNickname,
//   isEditMode = false,
//   showNicknameField = true,
// }) => {
//   const { theme } = useTheme();

//   // 커스텀 훅 사용
//   const { formData, errors, isNicknameAvailable, setIsNicknameAvailable, updateField, validate, setErrors } =
//     useProfileForm({
//       ...initialData,
//       nickname: isEditMode ? initialData.nickname || '' : '',
//     });

//   const [isCheckingNickname, setIsCheckingNickname] = React.useState(false);
//   const [isSubmitting, setIsSubmitting] = React.useState(false);

//   // 닉네임 중복 확인
//   const handleCheckNickname = async () => {
//     if (!onCheckNickname) return;

//     // 먼저 유효성 검사
//     if (errors.nickname) return;

//     setIsCheckingNickname(true);
//     try {
//       const isAvailable = await onCheckNickname(formData.nickname);
//       setIsNicknameAvailable(isAvailable);

//       if (!isAvailable) {
//         setErrors((prev) => ({ ...prev, nickname: '이미 사용 중인 닉네임입니다' }));
//       }
//     } catch (error) {
//       console.error('닉네임 확인 실패:', error);
//     } finally {
//       setIsCheckingNickname(false);
//     }
//   };

//   // 제출
//   const handleSubmit = async () => {
//     // 전체 검증
//     if (!validate()) return;

//     // 닉네임 중복 확인 체크 (신규 가입 시)
//     if (showNicknameField && !isEditMode && isNicknameAvailable === null) {
//       setErrors((prev) => ({ ...prev, nickname: '닉네임 중복 확인을 해주세요' }));
//       return;
//     }

//     if (showNicknameField && !isEditMode && !isNicknameAvailable) {
//       return;
//     }

//     setIsSubmitting(true);
//     try {
//       await onSubmit(formData);
//     } finally {
//       setIsSubmitting(false);
//     }
//   };

//   const isSubmitDisabled =
//     (showNicknameField && (!formData.nickname || !!errors.nickname || (!isEditMode && !isNicknameAvailable))) ||
//     !!errors.introduction ||
//     isSubmitting;

//   return (
//     <>
//       <ProfileImagePicker imageUri={formData.profileImage} onImageSelect={(uri) => updateField('profileImage', uri)} />

//       {showNicknameField && (
//         <View style={styles.inputSection}>
//           <Text style={[styles.label, { color: theme.colors.text.primary }]}>
//             닉네임<Text style={styles.required}>*</Text>
//           </Text>
//           <View style={styles.nicknameContainer}>
//             <TextInput
//               style={[
//                 styles.input,
//                 styles.nicknameInput,
//                 {
//                   color: theme.colors.text.primary,
//                   borderColor: errors.nickname ? theme.colors.error : theme.colors.border,
//                 },
//               ]}
//               value={formData.nickname}
//               onChangeText={(text) => updateField('nickname', text)}
//               placeholder='닉네임을 입력해주세요'
//               placeholderTextColor={theme.colors.text.disabled}
//               maxLength={10}
//               editable={!isEditMode}
//             />
//             {!isEditMode && onCheckNickname && (
//               <TouchableOpacity
//                 style={[styles.checkButton, { backgroundColor: theme.colors.text.primary }]}
//                 onPress={handleCheckNickname}
//                 disabled={!formData.nickname || !!errors.nickname || isCheckingNickname}
//               >
//                 {isCheckingNickname ? (
//                   <ActivityIndicator size='small' color='#FFFFFF' />
//                 ) : (
//                   <Text style={styles.checkButtonText}>중복확인</Text>
//                 )}
//               </TouchableOpacity>
//             )}
//           </View>
//           {errors.nickname && <Text style={[styles.errorText, { color: theme.colors.error }]}>{errors.nickname}</Text>}
//           {isNicknameAvailable && !isEditMode && (
//             <Text style={[styles.successText, { color: theme.colors.success }]}>사용 가능한 닉네임입니다</Text>
//           )}
//         </View>
//       )}

//       <View style={styles.inputSection}>
//         <Text style={[styles.label, { color: theme.colors.text.primary }]}>자기소개</Text>
//         <TextInput
//           style={[
//             styles.input,
//             styles.textArea,
//             {
//               color: theme.colors.text.primary,
//               borderColor: errors.introduction ? theme.colors.error : theme.colors.border,
//             },
//           ]}
//           value={formData.introduction}
//           onChangeText={(text) => updateField('introduction', text)}
//           placeholder='자기소개를 입력해주세요'
//           placeholderTextColor={theme.colors.text.disabled}
//           multiline
//           maxLength={50}
//           textAlignVertical='top'
//         />
//         <Text style={[styles.charCount, { color: theme.colors.text.secondary }]}>
//           {formData.introduction.length}/50
//         </Text>
//         {errors.introduction && (
//           <Text style={[styles.errorText, { color: theme.colors.error }]}>{errors.introduction}</Text>
//         )}
//       </View>

//       <TouchableOpacity
//         style={[styles.submitButton, { backgroundColor: isSubmitDisabled ? '#CCCCCC' : theme.colors.text.primary }]}
//         onPress={handleSubmit}
//         disabled={isSubmitDisabled}
//       >
//         {isSubmitting ? (
//           <ActivityIndicator size='small' color='#FFFFFF' />
//         ) : (
//           <Text style={styles.submitButtonText}>{isEditMode ? '변경하기' : '가입하기'}</Text>
//         )}
//       </TouchableOpacity>
//     </>
//   );
// };

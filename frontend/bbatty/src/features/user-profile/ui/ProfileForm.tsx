import React from 'react';
import { View, Text, TextInput, TouchableOpacity, ActivityIndicator } from 'react-native';
import { ProfileImagePicker } from './ProfileImagePicker';
import { useProfileForm } from '../hooks/useProfileForm';
import { styles } from './ProfileForm.style';
import { ProfileFormData } from '../model/profileTypes';

export interface ProfileFormProps {
  initialData?: Partial<ProfileFormData>;
  onSubmit: (data: ProfileFormData) => Promise<void>;
  isEditMode?: boolean;
  showNicknameField?: boolean;
}

export const ProfileForm: React.FC<ProfileFormProps> = ({
  initialData = {},
  onSubmit,
  isEditMode = false,
  showNicknameField = true,
}) => {
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  // 개선된 커스텀 훅 사용
  const { formData, errors, nicknameStatus, updateField, validate } = useProfileForm({
    ...initialData,
    nickname: isEditMode ? initialData.nickname || '' : '',
  });

  // 닉네임 입력 필드 스타일 결정
  const getNicknameInputStyle = () => {
    if (errors.nickname) {
      return [styles.input, styles.nicknameInput, styles.inputError];
    }

    if (nicknameStatus.showSuccess) {
      return [styles.input, styles.nicknameInput, styles.inputSuccess];
    }

    return [styles.input, styles.nicknameInput];
  };

  // 자기소개 입력 필드 스타일 결정
  const getIntroductionInputStyle = () => {
    if (errors.introduction) {
      return [styles.input, styles.textArea, styles.inputError];
    }

    return [styles.input, styles.textArea];
  };

  // 닉네임 상태 메시지 렌더링
  const renderNicknameStatus = () => {
    if (errors.nickname) {
      return <Text style={styles.errorText}>{errors.nickname}</Text>;
    }

    if (nicknameStatus.isChecking) {
      return (
        <View style={styles.nicknameStatusContainer}>
          <ActivityIndicator size='small' color='#666' />
          <Text style={styles.nicknameStatusText}>사용 가능 여부 확인 중...</Text>
        </View>
      );
    }

    if (nicknameStatus.showSuccess) {
      return <Text style={styles.successText}>사용 가능한 닉네임입니다</Text>;
    }

    return null;
  };

  // 제출 버튼 활성화 상태 결정
  const isSubmitDisabled = () => {
    if (isSubmitting) return true;

    // 닉네임 필드가 표시되는 경우
    if (showNicknameField) {
      // 편집 모드가 아닌 경우 (신규 가입)
      if (!isEditMode) {
        return !nicknameStatus.isValid || !nicknameStatus.isAvailable;
      }
      // 편집 모드인 경우
      return !nicknameStatus.isValid;
    }

    // 자기소개만 있는 경우
    return !!errors.introduction;
  };

  // 제출 버튼 스타일 결정
  const getSubmitButtonStyle = () => {
    const disabled = isSubmitDisabled();

    return [styles.submitButton, disabled ? styles.submitButtonDisabled : styles.submitButtonActive];
  };

  // 제출
  const handleSubmit = async () => {
    // 전체 검증
    if (!validate()) {
      console.log('유효성 검증 실패');
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(formData);
    } catch (error) {
      console.error('제출 실패:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <ProfileImagePicker imageUri={formData.profileImage} onImageSelect={(uri) => updateField('profileImage', uri)} />

      {showNicknameField && (
        <View style={styles.inputSection}>
          <Text style={styles.label}>
            닉네임<Text style={styles.required}>*</Text>
          </Text>
          <View style={styles.nicknameContainer}>
            <TextInput
              style={getNicknameInputStyle()}
              value={formData.nickname}
              onChangeText={(text) => updateField('nickname', text)}
              placeholder='2~10글자 사이로 입력해주세요'
              maxLength={10} // 하드 리미트
              editable={!isEditMode}
              autoCorrect={false}
              autoCapitalize='none'
            />
            {/* 편집 모드가 아닐 때만 중복 확인 상태 표시 */}
            {!isEditMode && nicknameStatus.isChecking && (
              <View style={styles.checkButton}>
                <ActivityIndicator size='small' color='#FFFFFF' />
              </View>
            )}
          </View>
          {renderNicknameStatus()}
        </View>
      )}

      <View style={styles.inputSection}>
        <Text style={styles.label}>자기소개</Text>
        <TextInput
          style={getIntroductionInputStyle()}
          value={formData.introduction}
          onChangeText={(text) => updateField('introduction', text)}
          placeholder='간단한 자기소개를 입력해주세요'
          multiline
          maxLength={50}
          textAlignVertical='top'
        />
        <View style={styles.introductionFooter}>
          {errors.introduction ? (
            <Text style={styles.errorText}>{errors.introduction}</Text>
          ) : (
            <View style={styles.introductionFooterEmpty} />
          )}
          <Text style={styles.charCount}>{formData.introduction?.length || 0}/50</Text>
        </View>
      </View>

      <TouchableOpacity style={getSubmitButtonStyle()} onPress={handleSubmit} disabled={isSubmitDisabled()}>
        {isSubmitting ? (
          <ActivityIndicator size='small' color='#FFFFFF' />
        ) : (
          <Text style={styles.submitButtonText}>{isEditMode ? '변경하기' : '가입하기'}</Text>
        )}
      </TouchableOpacity>
    </>
  );
};

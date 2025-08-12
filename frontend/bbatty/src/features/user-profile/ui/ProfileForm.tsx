import React from 'react';
import { View, Text, TextInput, TouchableOpacity, ActivityIndicator } from 'react-native';
import { ProfileImagePicker } from './ProfileImagePicker';
import { useProfileForm } from '../hooks/useProfileForm';
import { styles } from './ProfileForm.style';
import { ProfileFormData } from '../model/profileTypes';

export interface ProfileFormProps {
  initialData?: Partial<ProfileFormData>;
  onSubmit: (data: ProfileFormData) => Promise<void>;
  showNicknameField?: boolean;
  originalNickname?: string; // 편집 모드에서 기존 닉네임
}

export const ProfileForm: React.FC<ProfileFormProps> = ({
  initialData = {},
  onSubmit,
  showNicknameField = true,
  originalNickname,
}) => {
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  // 개선된 커스텀 훅 사용 - initialData가 이미 초기 상태로 설정됨
  const { formData, errors, nicknameStatus, updateField, handleCheckNickname, validate } = useProfileForm(initialData, originalNickname);

  // 현재 닉네임이 기존 닉네임과 동일한지 체크
  const isNicknameSameAsOriginal = () => {
    return originalNickname && formData.nickname === originalNickname;
  };

  // 현재 자기소개가 기존 자기소개와 동일한지 체크
  const isIntroductionSameAsOriginal = () => {
    return formData.introduction === (initialData?.introduction || '');
  };

  // 아무 필드도 변경되지 않았는지 체크
  const isDataUnchanged = () => {
    const nicknameUnchanged = originalNickname ? isNicknameSameAsOriginal() : formData.nickname === (initialData?.nickname || '');
    const introductionUnchanged = isIntroductionSameAsOriginal();
    const imageUnchanged = formData.profileImage === (initialData?.profileImage || '');
    
    return nicknameUnchanged && introductionUnchanged && imageUnchanged;
  };

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

    if (nicknameStatus.showSuccess) {
      return <Text style={styles.successText}>사용 가능한 닉네임입니다</Text>;
    }

    return null;
  };

  // 중복체크 버튼 스타일 결정
  const getCheckButtonStyle = () => {
    // 기존 닉네임과 동일하면 비활성화
    if (isNicknameSameAsOriginal()) {
      return [styles.checkButton, styles.checkButtonError];
    }

    if (!nicknameStatus.canCheck || nicknameStatus.isChecking) {
      return [styles.checkButton, styles.checkButtonError];
    }

    if (nicknameStatus.hasError) {
      return [styles.checkButton, styles.checkButtonError];
    }

    return [styles.checkButton, styles.checkButtonActive];
  };

  // 제출 버튼 활성화 상태 결정
  const isSubmitDisabled = () => {
    if (isSubmitting) return true;
    
    // 아무것도 변경되지 않았으면 비활성화
    if (isDataUnchanged()) return true;

    // 닉네임 필드가 표시되는 경우
    if (showNicknameField) {
      // 기존 닉네임과 동일하면 중복체크 없이도 제출 가능
      if (isNicknameSameAsOriginal()) {
        return !!errors.nickname; // 자기소개 에러는 제외 (자동 조정되므로)
      }
      // 기존 닉네임과 다르면 중복체크 완료 필요
      return !nicknameStatus.isAvailable;
    }

    // 자기소개만 있는 경우 - 자기소개 에러는 버튼 비활성화에 영향 안 줌
    return false;
  };

  // 제출 버튼 스타일 결정 - 중복체크 버튼과 같은 색상 체계
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
              maxLength={20} // 10글자 초과 입력 감지를 위해 여유있게 설정
              editable={true}
              autoCorrect={false}
              autoCapitalize='none'
            />
            <TouchableOpacity
              style={getCheckButtonStyle()}
              onPress={handleCheckNickname}
              disabled={isNicknameSameAsOriginal() || !nicknameStatus.canCheck || nicknameStatus.isChecking}
            >
              {nicknameStatus.isChecking ? (
                <ActivityIndicator size='small' color='#FFFFFF' />
              ) : (
                <Text style={styles.checkButtonText}>중복확인</Text>
              )}
            </TouchableOpacity>
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
          <Text style={styles.submitButtonText}>{originalNickname ? '변경하기' : '가입하기'}</Text>
        )}
      </TouchableOpacity>
    </>
  );
};

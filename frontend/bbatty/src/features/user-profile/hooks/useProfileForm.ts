import { useState, useCallback } from 'react';
import { ValidationRules, combineValidators, nicknameValidator, filterNicknameInput } from '../../../shared';
import { ProfileFormData } from '../model/profileTypes';
import { profileApi } from '../api/profileApi';
import { isOk } from '../../../shared/utils/result';

// 오류 타입 정의
type NicknameErrorType = 'LENGTH_OVER' | 'SPECIAL_CHARS' | 'VALIDATION' | 'NONE';

export const useProfileForm = (initialData?: Partial<ProfileFormData>) => {
  const [formData, setFormData] = useState<ProfileFormData>({
    nickname: initialData?.nickname || '',
    profileImage: initialData?.profileImage,
    introduction: initialData?.introduction || '',
  });

  const [errors, setErrors] = useState<Partial<Record<keyof ProfileFormData, string>>>({});
  const [isNicknameAvailable, setIsNicknameAvailable] = useState<boolean | null>(null);
  const [isCheckingNickname, setIsCheckingNickname] = useState(false);

  // 닉네임 오류 타입 상태 추가
  const [nicknameErrorType, setNicknameErrorType] = useState<NicknameErrorType>('NONE');

  // 검증 규칙
  const validators = {
    nickname: nicknameValidator,
    introduction: ValidationRules.maxLength(50),
  };

  // 중복 확인 버튼 클릭 핸들러
  const handleCheckNickname = useCallback(async () => {
    // 실제 표시된 값이 유효한지만 체크 (경고 메시지는 무시)
    if (!formData.nickname || formData.nickname.length < 2 || formData.nickname.length > 10) {
      return;
    }

    setIsCheckingNickname(true);
    try {
      const result = await profileApi.checkNickname({ nickname: formData.nickname });

      if (isOk(result)) {
        console.log('닉네임 체크 성공:', result.data);
        setIsNicknameAvailable(result.data.available);

        if (result.data.available) {
          // 중복 확인 성공 시 모든 경고 메시지 지우고 오류 타입 리셋
          setNicknameErrorType('NONE');
          setErrors((prev) => ({
            ...prev,
            nickname: undefined,
          }));
        } else {
          // 중복된 닉네임인 경우
          setErrors((prev) => ({
            ...prev,
            nickname: '이미 사용 중인 닉네임입니다',
          }));
        }
      } else {
        console.error('닉네임 체크 실패:', result.error);
        setErrors((prev) => ({
          ...prev,
          nickname: '닉네임 확인에 실패했습니다',
        }));
        setIsNicknameAvailable(null);
      }
    } catch (error) {
      console.error('예외 발생:', error);
      setErrors((prev) => ({
        ...prev,
        nickname: '닉네임 확인 중 오류가 발생했습니다',
      }));
      setIsNicknameAvailable(null);
    } finally {
      setIsCheckingNickname(false);
    }
  }, [formData.nickname]);

  // 닉네임 실시간 필터링 및 업데이트
  const updateNickname = useCallback(
    (rawValue: string) => {
      // 1. 원본 값으로 문제 상황 체크 (필터링 전에 먼저 체크)
      const isOverLength = rawValue.length > 10;
      const hasSpecialChars = /[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9]/.test(rawValue);

      // 2. 실시간 필터링 (화면에는 필터링된 값 표시)
      const { value: filteredValue } = filterNicknameInput(rawValue);

      // 3. 상태 업데이트
      setFormData((prev) => ({ ...prev, nickname: filteredValue }));

      // 4. 중복 확인 상태 초기화 (닉네임이 변경되면 다시 확인 필요)
      setIsNicknameAvailable(null);

      // 5. 오류 타입 및 메시지 결정
      if (isOverLength) {
        setNicknameErrorType('LENGTH_OVER');
        setErrors((prev) => ({
          ...prev,
          nickname: '2~10글자 사이로 입력해주세요',
        }));
      } else if (hasSpecialChars) {
        setNicknameErrorType('SPECIAL_CHARS');
        setErrors((prev) => ({
          ...prev,
          nickname: '한글, 영문, 숫자만 사용 가능합니다 (특수기호, 공백 금지)',
        }));
      } else {
        // 정상적인 입력인 경우에만 오류 타입 초기화 및 일반 검증
        if (nicknameErrorType !== 'NONE') {
          setNicknameErrorType('NONE');
        }

        // 일반 유효성 검증
        const validation = validators.nickname(filteredValue);
        setErrors((prev) => ({
          ...prev,
          nickname: validation.error,
        }));
      }
    },
    [validators.nickname, nicknameErrorType]
  );

  // 일반 필드 업데이트
  const updateField = useCallback(
    <K extends keyof ProfileFormData>(field: K, value: ProfileFormData[K]) => {
      if (field === 'nickname') {
        updateNickname(value as string);
        return;
      }

      setFormData((prev) => ({ ...prev, [field]: value }));

      // 검증
      if (field in validators) {
        const validator = validators[field as keyof typeof validators];
        const validation = validator(value as string);
        setErrors((prev) => ({
          ...prev,
          [field]: validation.error,
        }));
      }
    },
    [updateNickname, validators]
  );

  // 전체 검증
  const validate = useCallback((): boolean => {
    const newErrors: typeof errors = {};

    // 각 필드 검증
    (Object.keys(validators) as Array<keyof typeof validators>).forEach((field) => {
      const validation = validators[field](formData[field] as string);
      if (validation.error) {
        newErrors[field] = validation.error;
      }
    });

    // 닉네임 중복 확인 상태 체크
    if (formData.nickname && isNicknameAvailable === false) {
      newErrors.nickname = '이미 사용 중인 닉네임입니다';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0 && isNicknameAvailable === true;
  }, [formData, validators, isNicknameAvailable]);

  // 닉네임 상태 계산
  const nicknameStatus = {
    isValid: !errors.nickname && formData.nickname.length >= 2,
    isAvailable: isNicknameAvailable === true,
    isChecking: isCheckingNickname,
    showSuccess: isNicknameAvailable === true && !errors.nickname,
    // 중복 확인 버튼 활성화 조건: 현재 표시된 값이 2-10글자면 항상 체크 가능
    canCheck: formData.nickname.length >= 2 && formData.nickname.length <= 10,
    // 에러 판정: 현재 표시된 값 기준으로만 판단 (오버타이핑 경고는 무시)
    hasError: formData.nickname.length < 2 || formData.nickname.length > 10 || isNicknameAvailable === false,
  };

  return {
    formData,
    errors,
    isNicknameAvailable,
    nicknameStatus,
    updateField,
    handleCheckNickname, // 중복 확인 버튼용 핸들러
    validate,
    setErrors,
  };
};

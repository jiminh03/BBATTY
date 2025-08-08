import { useState } from 'react';
import { ValidationRules, combineValidators } from '../../../shared';
import { ProfileFormData } from '../model/profileTypes';

export const useProfileForm = (initialData?: Partial<ProfileFormData>) => {
  const [formData, setFormData] = useState<ProfileFormData>({
    nickname: initialData?.nickname || '',
    profileImage: initialData?.profileImage,
    introduction: initialData?.introduction,
  });

  const [errors, setErrors] = useState<Partial<Record<keyof ProfileFormData, string>>>({});
  const [isNicknameAvailable, setIsNicknameAvailable] = useState<boolean | null>(null);

  // 검증 규칙
  const validators = {
    nickname: combineValidators(
      ValidationRules.required('닉네임을 입력해주세요'),
      ValidationRules.minLength(2),
      ValidationRules.maxLength(10)
    ),
    introduction: ValidationRules.maxLength(50),
  };

  // 필드 업데이트
  const updateField = <K extends keyof ProfileFormData>(field: K, value: ProfileFormData[K]) => {
    setFormData((prev) => ({ ...prev, [field]: value }));

    // 닉네임 변경 시 중복 확인 초기화
    if (field === 'nickname') {
      setIsNicknameAvailable(null);
    }

    // 검증
    if (field in validators) {
      const validator = validators[field as keyof typeof validators];
      const validation = validator(value as string);
      setErrors((prev) => ({
        ...prev,
        [field]: validation.error || undefined,
      }));
    }
  };

  // 전체 검증
  const validate = (): boolean => {
    const newErrors: typeof errors = {};

    // 각 필드 검증
    (Object.keys(validators) as Array<keyof typeof validators>).forEach((field) => {
      const validation = validators[field](formData[field] as string);
      if (validation.error) {
        newErrors[field] = validation.error;
      }
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  return {
    formData,
    errors,
    isNicknameAvailable,
    setIsNicknameAvailable,
    updateField,
    validate,
    setErrors,
  };
};

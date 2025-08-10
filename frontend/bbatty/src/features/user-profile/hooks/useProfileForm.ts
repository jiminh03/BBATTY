import { useState, useCallback, useRef, useEffect } from 'react';
import { ValidationRules, combineValidators, nicknameValidator, filterNicknameInput } from '../../../shared';
import { ProfileFormData } from '../model/profileTypes';
import { profileApi } from '../api/profileApi';
import { isOk } from '../../../shared/utils/result';

export const useProfileForm = (initialData?: Partial<ProfileFormData>) => {
  const [formData, setFormData] = useState<ProfileFormData>({
    nickname: initialData?.nickname || '',
    profileImage: initialData?.profileImage,
    introduction: initialData?.introduction || '',
  });

  const [errors, setErrors] = useState<Partial<Record<keyof ProfileFormData, string>>>({});
  const [isNicknameAvailable, setIsNicknameAvailable] = useState<boolean | null>(null);
  const [isCheckingNickname, setIsCheckingNickname] = useState(false);

  // 디바운싱을 위한 타이머 참조 - 수정된 타입
  const nicknameCheckTimer = useRef<any>(undefined);
  const lastCheckedNickname = useRef<string>('');

  // 검증 규칙
  const validators = {
    nickname: nicknameValidator,
    introduction: ValidationRules.maxLength(50),
  };

  // 닉네임 중복 확인 API 호출 - 실제 API 연결
  const checkNicknameAvailability = useCallback(async (nickname: string): Promise<boolean> => {
    try {
      const result = await profileApi.checkNickname({ nickname });

      if (isOk(result)) {
        console.log('닉네임 체크 성공:', result.data);
        return result.data.available;
      } else {
        console.error('닉네임 체크 실패:', result.error);
        throw new Error(result.error.message || '닉네임 확인에 실패했습니다');
      }
    } catch (error) {
      console.error('닉네임 중복 확인 실패:', error);
      throw error;
    }
  }, []);

  // 디바운싱된 닉네임 중복 확인
  const debouncedNicknameCheck = useCallback(
    (nickname: string) => {
      // 이전 타이머 취소
      if (nicknameCheckTimer.current) {
        clearTimeout(nicknameCheckTimer.current);
      }

      // 빈 값이거나 유효하지 않으면 중복 확인 안함
      const validation = validators.nickname(nickname);
      if (!nickname || !validation.isValid) {
        setIsNicknameAvailable(null);
        setIsCheckingNickname(false);
        return;
      }

      // 이미 확인한 닉네임이면 skip
      if (nickname === lastCheckedNickname.current) {
        return;
      }

      setIsCheckingNickname(true);

      // 1초 후 중복 확인 실행
      nicknameCheckTimer.current = setTimeout(async () => {
        try {
          const isAvailable = await checkNicknameAvailability(nickname);
          setIsNicknameAvailable(isAvailable);
          lastCheckedNickname.current = nickname;

          if (!isAvailable) {
            setErrors((prev) => ({
              ...prev,
              nickname: '이미 사용 중인 닉네임입니다',
            }));
          }
        } catch (error) {
          setErrors((prev) => ({
            ...prev,
            nickname: '닉네임 확인 중 오류가 발생했습니다',
          }));
          setIsNicknameAvailable(null);
        } finally {
          setIsCheckingNickname(false);
        }
      }, 1000);
    },
    [checkNicknameAvailability, validators.nickname]
  );

  // 닉네임 실시간 필터링 및 업데이트
  const updateNickname = useCallback(
    (rawValue: string) => {
      // 1. 실시간 필터링
      const { value: filteredValue, hasInvalid } = filterNicknameInput(rawValue);

      // 2. 상태 업데이트
      setFormData((prev) => ({ ...prev, nickname: filteredValue }));

      // 3. 필터링 경고 메시지
      if (hasInvalid) {
        setErrors((prev) => ({
          ...prev,
          nickname: '한글, 영문, 숫자만 사용 가능합니다 (특수기호, 공백 금지)',
        }));

        // 2초 후 경고 메시지 제거 (유효성 검증 메시지로 대체)
        setTimeout(() => {
          const validation = validators.nickname(filteredValue);
          setErrors((prev) => ({
            ...prev,
            nickname: validation.error,
          }));
        }, 2000);
      } else {
        // 4. 일반 유효성 검증
        const validation = validators.nickname(filteredValue);
        setErrors((prev) => ({
          ...prev,
          nickname: validation.error,
        }));
      }

      // 5. 중복 확인 상태 초기화 및 디바운싱 시작
      setIsNicknameAvailable(null);
      debouncedNicknameCheck(filteredValue);
    },
    [validators.nickname, debouncedNicknameCheck]
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
  };

  // 컴포넌트 언마운트 시 타이머 정리
  useEffect(() => {
    return () => {
      if (nicknameCheckTimer.current) {
        clearTimeout(nicknameCheckTimer.current);
      }
    };
  }, []);

  return {
    formData,
    errors,
    isNicknameAvailable,
    nicknameStatus,
    updateField,
    validate,
    setErrors,
  };
};

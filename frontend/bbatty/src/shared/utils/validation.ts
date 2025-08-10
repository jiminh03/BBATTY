// shared/utils/validation.ts

// 검증 결과 타입
export interface ValidationResult {
  isValid: boolean;
  error?: string;
  filteredValue?: string; // 필터링된 값 (실시간 입력 시 사용)
}

// 검증 규칙 타입
export type ValidationRule<T = string> = (value: T) => ValidationResult;

// 닉네임 전용 필터링 함수
export const filterNicknameInput = (input: string): { value: string; hasInvalid: boolean } => {
  const original = input;

  // 1. 허용된 문자만 추출 (한글, 영문, 숫자)
  const filtered = input.replace(/[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9]/g, '');

  // 2. 길이 제한 (10글자)
  const lengthLimited = filtered.slice(0, 10);

  return {
    value: lengthLimited,
    hasInvalid: original !== lengthLimited, // 필터링이 발생했는지 확인
  };
};

// 기본 검증 규칙들
export const ValidationRules = {
  // 필수 입력
  required:
    (message = '필수 입력 항목입니다'): ValidationRule =>
    (value) => ({
      isValid: !!value && value.trim().length > 0,
      error: !value || value.trim().length === 0 ? message : undefined,
    }),

  // 최소 길이
  minLength:
    (min: number, message?: string): ValidationRule =>
    (value) => ({
      isValid: value.length >= min,
      error: value.length < min ? message || `최소 ${min}자 이상 입력해주세요` : undefined,
    }),

  // 최대 길이
  maxLength:
    (max: number, message?: string): ValidationRule =>
    (value) => ({
      isValid: value.length <= max,
      error: value.length > max ? message || `최대 ${max}자까지 입력 가능합니다` : undefined,
    }),

  // 길이 범위 (닉네임용)
  lengthRange:
    (min: number, max: number, message?: string): ValidationRule =>
    (value) => ({
      isValid: value.length >= min && value.length <= max,
      error: value.length < min || value.length > max ? message || `${min}~${max}글자 사이로 입력해주세요` : undefined,
    }),

  // 닉네임 전용 검증 (한글, 영문, 숫자만 + 특수문자/공백 금지)
  nicknameFormat:
    (message = '한글, 영문, 숫자만 사용 가능합니다 (특수기호, 공백 금지)'): ValidationRule =>
    (value) => {
      // 허용된 문자만 확인
      const isValidFormat = /^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9]*$/.test(value);
      return {
        isValid: isValidFormat,
        error: !isValidFormat ? message : undefined,
      };
    },

  // 한글 초성 금지
  noIncompleteKorean:
    (message = '완성된 한글만 입력해주세요'): ValidationRule =>
    (value) => {
      const hasIncompleteKorean = /[ㄱ-ㅎㅏ-ㅣ]/.test(value);
      return {
        isValid: !hasIncompleteKorean,
        error: hasIncompleteKorean ? message : undefined,
      };
    },

  // 연속 문자 방지 (같은 문자 3번 이상)
  noRepeatedChars:
    (maxRepeat = 2, message = '같은 문자를 3번 이상 연속 사용할 수 없습니다'): ValidationRule =>
    (value) => {
      const hasRepeated = new RegExp(`(.)\\1{${maxRepeat},}`).test(value);
      return {
        isValid: !hasRepeated,
        error: hasRepeated ? message : undefined,
      };
    },

  // 의미없는 패턴 방지
  noMeaninglessPattern:
    (message = '의미있는 닉네임을 입력해주세요'): ValidationRule =>
    (value) => {
      const meaninglessPatterns = [
        /^(asdf|qwer|zxcv|1234|abcd)+$/i,
        /^(ㄱㄴㄷㄹ|ㅁㅂㅅㅇ)+$/,
        /^[0-9]+$/, // 숫자만
        /^(.)\1+$/, // 모두 같은 문자
      ];

      const isMeaningless = meaninglessPatterns.some((pattern) => pattern.test(value));
      return {
        isValid: !isMeaningless,
        error: isMeaningless ? message : undefined,
      };
    },

  // 이모지 및 특수 유니코드 방지
  noEmoji:
    (message = '이모지는 사용할 수 없습니다'): ValidationRule =>
    (value) => {
      // 이모지 및 특수 유니코드 패턴
      const emojiRegex =
        /[\u{1F600}-\u{1F64F}]|[\u{1F300}-\u{1F5FF}]|[\u{1F680}-\u{1F6FF}]|[\u{1F1E0}-\u{1F1FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/gu;
      const hasEmoji = emojiRegex.test(value);
      return {
        isValid: !hasEmoji,
        error: hasEmoji ? message : undefined,
      };
    },

  // 숫자만
  numeric:
    (message = '숫자만 입력 가능합니다'): ValidationRule =>
    (value) => ({
      isValid: /^\d*$/.test(value),
      error: !/^\d*$/.test(value) ? message : undefined,
    }),

  // 한글만
  korean:
    (message = '한글만 입력 가능합니다'): ValidationRule =>
    (value) => ({
      isValid: /^[가-힣\s]*$/.test(value),
      error: !/^[가-힣\s]*$/.test(value) ? message : undefined,
    }),
};

// 여러 검증 규칙 조합
export const combineValidators = <T = string>(...validators: ValidationRule<T>[]): ValidationRule<T> => {
  return (value: T): ValidationResult => {
    for (const validator of validators) {
      const result = validator(value);
      if (!result.isValid) {
        return result;
      }
    }
    return { isValid: true };
  };
};

// 닉네임 전용 통합 검증 (연속 문자 검증 제거)
export const nicknameValidator = combineValidators(
  ValidationRules.required('닉네임을 입력해주세요'),
  ValidationRules.lengthRange(2, 10),
  ValidationRules.nicknameFormat(),
  ValidationRules.noIncompleteKorean(),
  ValidationRules.noMeaninglessPattern(),
  ValidationRules.noEmoji()
);

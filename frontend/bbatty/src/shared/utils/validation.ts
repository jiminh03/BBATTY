// 검증 결과 타입
export interface ValidationResult {
  isValid: boolean;
  error?: string;
}

// 검증 규칙 타입
export type ValidationRule<T = string> = (value: T) => ValidationResult;

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

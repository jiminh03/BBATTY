import { MessageValidation } from '../model/types';

export const messageValidation = {
  // 메시지 유효성 검증
  validateMessage: (content: string): MessageValidation => {
    const errors: string[] = [];

    // 빈 메시지 체크
    if (!content || content.trim().length === 0) {
      errors.push('메시지를 입력해주세요.');
    }

    // 길이 체크
    if (content.length > 500) {
      errors.push('메시지는 500자 이하로 입력해주세요.');
    }

    // 연속 공백 체크
    if (/\s{10,}/.test(content)) {
      errors.push('연속된 공백이 너무 많습니다.');
    }

    // 금지 단어 체크 (예시)
    const bannedWords = ['욕설1', '욕설2', '스팸'];
    const hasBannedWord = bannedWords.some(word => 
      content.toLowerCase().includes(word.toLowerCase())
    );
    
    if (hasBannedWord) {
      errors.push('부적절한 내용이 포함되어 있습니다.');
    }

    // URL 스팸 체크
    const urlPattern = /(https?:\/\/[^\s]+)/gi;
    const urls = content.match(urlPattern);
    if (urls && urls.length > 2) {
      errors.push('링크는 2개까지만 포함할 수 있습니다.');
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  },

  // 메시지 정제
  sanitizeMessage: (content: string): string => {
    return content
      .trim()
      .replace(/\s+/g, ' ') // 연속 공백을 하나로
      .replace(/\n{3,}/g, '\n\n'); // 연속 줄바꿈 제한
  },

  // 레이트 리미팅 체크
  checkRateLimit: (sendCount: number, timeWindowMs: number = 60000, limit: number = 10): boolean => {
    // 실제로는 서버에서 처리하지만, 클라이언트에서도 기본적인 체크
    return sendCount >= limit;
  },
};
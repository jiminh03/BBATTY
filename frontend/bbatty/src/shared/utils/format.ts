// 숫자 포맷팅
export const NumberFormat = {
  // 천 단위 구분
  withCommas: (num: number | string): string => {
    const number = typeof num === 'string' ? parseFloat(num) : num;
    if (isNaN(number)) return '0';

    return number.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  },

  // 퍼센트 표시
  toPercent: (value: number, decimals: number = 0): string => {
    return `${(value * 100).toFixed(decimals)}%`;
  },

  // 소수점 처리
  toFixed: (num: number, decimals: number = 2): string => {
    return num.toFixed(decimals);
  },

  // 순위 표시
  toOrdinal: (num: number): string => {
    return `${num}위`;
  },
};

// 문자열 포맷팅
export const StringFormat = {
  // 공백 제거
  removeWhitespace: (str: string): string => {
    return str.replace(/\s+/g, '');
  },
};

// 야구 관련 포맷팅
export const BaseballFormat = {
  // 승률 표시
  winRate: (wins: number, losses: number): string => {
    if (wins + losses === 0) return '0.000';
    const rate = wins / (wins + losses);
    return rate.toFixed(3);
  },
};

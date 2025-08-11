export const Format = {
  // 숫자 포맷팅
  number: {
    // 천 단위 구분
    withCommas: (num: number | string): string => {
      const number = typeof num === 'string' ? parseFloat(num) : num;
      if (isNaN(number)) return '0';
      return number.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    },

    // 소수점 처리
    toFixed: (num: number, decimals: number = 2): string => {
      return num.toFixed(decimals);
    },

    // 순위 표시
    toOrdinal: (num: number): string => {
      return `${num}위`;
    },
  },

  // 퍼센트 포맷팅
  percent: {
    // 기본 퍼센트 (소수점 없음)
    basic: (value: number, total: number): number => {
      if (total === 0) return 0;
      return Math.round((value / total) * 100);
    },

    // 소수점 있는 퍼센트
    decimal: (value: number, decimals: number = 0): string => {
      return `${(value * 100).toFixed(decimals)}%`;
    },

    // 정수 퍼센트 문자열
    string: (value: number, total: number): string => {
      return `${Format.percent.basic(value, total)}%`;
    },
  },

  // 승률 포맷팅
  winRate: {
    // API 응답 승률을 퍼센트로 (문자열/숫자 대응)
    toPercent: (winRate: string | number): number => {
      if (typeof winRate === 'string') {
        return Math.round(parseFloat(winRate) * 100);
      }
      return Math.round(winRate * 100);
    },

    // 승/패로 승률 계산 (소수점 3자리)
    calculate: (wins: number, losses: number): string => {
      if (wins + losses === 0) return '0.000';
      const rate = wins / (wins + losses);
      return rate.toFixed(3);
    },

    // 승률 계산 후 퍼센트 변환
    calculatePercent: (wins: number, total: number): number => {
      return Format.percent.basic(wins, total);
    },
  },

  // 문자열 포맷팅
  string: {
    // 공백 제거
    removeWhitespace: (str: string): string => {
      return str.replace(/\s+/g, '');
    },
  },

  // 야구/게임 관련 포맷팅
  game: {
    // 게임 수 포맷팅 (천 단위 구분)
    count: (count: number): string => {
      return Format.number.withCommas(count);
    },

    // 연속 기록 포맷팅
    streak: (count: number, type: 'win' | 'lose'): string => {
      const typeText = type === 'win' ? '연승' : '연패';
      return `${count}${typeText}`;
    },
  },
};

export const DATE_FORMATS = {
  FULL: 'YYYY.MM.DD',
  TIME: 'HH:mm',
  DATETIME: 'YYYY.MM.DD HH:mm',
  RELATIVE: 'relative', // ex) 방금 전, 1시간 전 등
} as const;

export type DATE_FORMAT = (typeof DATE_FORMATS)[keyof typeof DATE_FORMATS];

export const WEEKDAYS = {
  SHORT: ['일', '월', '화', '수', '목', '금', '토'],
  FULL: ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'],
} as const;

export const formatDate = (date: Date | string | number, format: DATE_FORMAT = DATE_FORMATS.FULL): string => {
  const d = new Date(date);

  if (isNaN(d.getTime())) {
    return '잘못된 날짜';
  }

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');

  const formatMap: Record<DATE_FORMAT, string> = {
    [DATE_FORMATS.FULL]: `${year}.${month}.${day}`,
    [DATE_FORMATS.TIME]: `${hours}:${minutes}`,
    [DATE_FORMATS.DATETIME]: `${year}.${month}.${day} ${hours}:${minutes}`,
    [DATE_FORMATS.RELATIVE]: getRelativeTime(d),
  };

  return (
    formatMap[format] ||
    format
      .replace('YYYY', String(year))
      .replace('MM', month)
      .replace('DD', day)
      .replace('HH', hours)
      .replace('mm', minutes)
  );
};

// 상대 시간 계산
export const getRelativeTime = (date: Date | string | number): string => {
  const d = new Date(date);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffSeconds = Math.floor(diffMs / 1000);
  const diffMinutes = Math.floor(diffSeconds / 60);
  const diffHours = Math.floor(diffMinutes / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffSeconds < 60) {
    return '방금 전';
  } else if (diffMinutes < 60) {
    return `${diffMinutes}분 전`;
  } else if (diffHours < 24) {
    return `${diffHours}시간 전`;
  } else if (diffDays < 7) {
    return `${diffDays}일 전`;
  } else if (diffDays < 30) {
    const weeks = Math.floor(diffDays / 7);
    return `${weeks}주 전`;
  } else if (diffDays < 365) {
    const months = Math.floor(diffDays / 30);
    return `${months}개월 전`;
  } else {
    const years = Math.floor(diffDays / 365);
    return `${years}년 전`;
  }
};

// 날짜 비교 함수들
export const isSameDay = (date1: Date, date2: Date): boolean => {
  return (
    date1.getFullYear() === date2.getFullYear() &&
    date1.getMonth() === date2.getMonth() &&
    date1.getDate() === date2.getDate()
  );
};

export const isToday = (date: Date): boolean => {
  return isSameDay(date, new Date());
};

// 요일 계산
export const getWeekday = (date: Date, format: 'SHORT' | 'FULL' = 'SHORT'): string => {
  return WEEKDAYS[format][date.getDay()];
};

// 날짜 범위 생성
export const getDateRange = (startDate: Date, endDate: Date): Date[] => {
  const dates: Date[] = [];
  const currentDate = new Date(startDate);

  while (currentDate <= endDate) {
    dates.push(new Date(currentDate));
    currentDate.setDate(currentDate.getDate() + 1);
  }

  return dates;
};

// ISO 날짜 문자열 파싱
export const parseISODate = (isoString: string): Date | null => {
  try {
    const date = new Date(isoString);
    return isNaN(date.getTime()) ? null : date;
  } catch {
    return null;
  }
};

// ================================ 시즌 관련 함수들 ===============================

// 서비스 시작년도
const SERVICE_START_YEAR = 2025;

export type Season = 'total' | string;

export const generateSeasons = (): Season[] => {
  const currentYear = new Date().getFullYear();
  const seasons: Season[] = ['total'];

  for (let year = SERVICE_START_YEAR; year <= currentYear; year++) {
    seasons.push(year.toString());
  }

  return seasons;
};

export const formatSeasonDisplay = (season: Season): string => {
  if (season === 'total') return '전체';
  return `${season}시즌`;
};

export const getCurrentSeason = (): string => {
  return new Date().getFullYear().toString();
};

export const isValidSeason = (season: string): boolean => {
  if (season === 'total') return true;

  const year = parseInt(season);
  const currentYear = new Date().getFullYear();

  return year >= SERVICE_START_YEAR && year <= currentYear;
};

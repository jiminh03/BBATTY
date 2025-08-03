import { Platform } from 'react-native';

// KBO 팀 컬러 정의
export const TEAM_COLORS = {
  LG: {
    primary: '#C30452',
    secondary: '#000000',
    accent: '#FFFFFF',
  },
  KT: {
    primary: '#000000',
    secondary: '#FF0000',
    accent: '#FFFFFF',
  },
  SSG: {
    primary: '#CE0E2D',
    secondary: '#FDB827',
    accent: '#FFFFFF',
  },
  NC: {
    primary: '#1D467F',
    secondary: '#B89968',
    accent: '#FFFFFF',
  },
  DOOSAN: {
    primary: '#131230',
    secondary: '#ED1C24',
    accent: '#FFFFFF',
  },
  KIA: {
    primary: '#EA0029',
    secondary: '#000000',
    accent: '#FFFFFF',
  },
  LOTTE: {
    primary: '#002955',
    secondary: '#FF0000',
    accent: '#FFFFFF',
  },
  SAMSUNG: {
    primary: '#0066B3',
    secondary: '#000000',
    accent: '#FFFFFF',
  },
  HANWHA: {
    primary: '#FF6600',
    secondary: '#000000',
    accent: '#FFFFFF',
  },
  KIWOOM: {
    primary: '#820024',
    secondary: '#000000',
    accent: '#FFFFFF',
  },
} as const;

export type TeamName = keyof typeof TEAM_COLORS;

// 기본 색상 팔레트
export const COLORS = {
  // 기본 색상
  primary: '#1D467F', // 기본값 (NC 색상)
  secondary: '#B89968',
  accent: '#FFFFFF',

  // 배경 색상
  background: '#F5F5F5',
  surface: '#FFFFFF',
  surfaceVariant: '#F8F8F8',

  // 텍스트 색상
  text: {
    primary: '#1A1A1A',
    secondary: '#666666',
    disabled: '#999999',
    inverse: '#FFFFFF',
  },

  // 상태 색상
  success: '#4CAF50',
  error: '#F44336',
  warning: '#FF9800',
  info: '#2196F3',

  // 테두리 색상
  border: '#E0E0E0',
  divider: '#F0F0F0',

  // 오버레이
  overlay: 'rgba(0, 0, 0, 0.5)',
} as const;

// 타이포그래피
export const TYPOGRAPHY = {
  fontFamily: {
    regular: Platform.select({
      ios: 'System',
      android: 'Roboto',
      default: 'System',
    }),
    medium: Platform.select({
      ios: 'System',
      android: 'Roboto-Medium',
      default: 'System',
    }),
    bold: Platform.select({
      ios: 'System',
      android: 'Roboto-Bold',
      default: 'System',
    }),
  },
  fontSize: {
    xs: 12,
    sm: 14,
    base: 16,
    lg: 18,
    xl: 20,
    xxl: 24,
    xxxl: 32,
  },
  lineHeight: {
    tight: 1.2,
    normal: 1.5,
    relaxed: 1.8,
  },
} as const;

// 간격
export const SPACING = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
} as const;

// 테두리 반경
export const RADIUS = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  full: 9999,
} as const;

// 그림자 (안드로이드/iOS 대응)
export const SHADOWS = {
  sm: {
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 2,
      },
      android: {
        elevation: 2,
      },
      default: {},
    }),
  },
  md: {
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
      },
      android: {
        elevation: 4,
      },
      default: {},
    }),
  },
  lg: {
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.15,
        shadowRadius: 8,
      },
      android: {
        elevation: 8,
      },
      default: {},
    }),
  },
} as const;

// Z-Index 레벨
export const Z_INDEX = {
  base: 0,
  dropdown: 100,
  sticky: 200,
  fixed: 300,
  modal: 400,
  popover: 500,
  tooltip: 600,
  toast: 700,
} as const;

// 팀 색상 타입
export type TeamColors = (typeof TEAM_COLORS)[TeamName];

// 동적 색상 타입
export interface DynamicColors extends Omit<typeof COLORS, 'primary' | 'secondary' | 'accent'> {
  primary: string;
  secondary: string;
  accent: string;
  team: TeamColors;
}

// 기본 테마 타입
export interface Theme {
  colors: DynamicColors;
  typography: typeof TYPOGRAPHY;
  spacing: typeof SPACING;
  radius: typeof RADIUS;
  shadows: typeof SHADOWS;
  zIndex: typeof Z_INDEX;
}

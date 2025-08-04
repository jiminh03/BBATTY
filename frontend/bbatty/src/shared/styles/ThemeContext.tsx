import React, { createContext, useContext, useMemo, ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import {
  COLORS,
  TEAM_COLORS,
  TYPOGRAPHY,
  SPACING,
  RADIUS,
  SHADOWS,
  Z_INDEX,
  type Theme,
  type TeamName,
  type DynamicColors,
} from './theme';

interface ThemeContextValue {
  theme: Theme;
  teamName: TeamName | null;
  setTeamName: (team: TeamName) => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

interface ThemeProviderProps {
  children: ReactNode;
  initialTeam?: TeamName;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children, initialTeam = null }) => {
  const [teamName, setTeamName] = React.useState<TeamName | null>(initialTeam);

  const theme = useMemo(() => {
    const teamColors = teamName
      ? TEAM_COLORS[teamName]
      : {
          primary: COLORS.primary,
          secondary: COLORS.secondary,
          accent: COLORS.accent,
        };

    const dynamicColors: DynamicColors = {
      ...COLORS,
      team: teamColors,
      // 팀 컬러로 기본 색상 오버라이드
      primary: teamColors.primary,
      secondary: teamColors.secondary,
      accent: teamColors.accent,
    };

    const themeObject: Theme = {
      colors: dynamicColors,
      typography: TYPOGRAPHY,
      spacing: SPACING,
      radius: RADIUS,
      shadows: SHADOWS,
      zIndex: Z_INDEX,
    };

    return themeObject;
  }, [teamName]);

  const value = useMemo(
    () => ({
      theme,
      teamName,
      setTeamName,
    }),
    [theme, teamName]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

// 테마 훅
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

// 스타일 생성 헬퍼 훅
export const useStyles = <T extends StyleSheet.NamedStyles<T>>(stylesFn: (theme: Theme) => T): T => {
  const { theme } = useTheme();
  return useMemo(() => StyleSheet.create(stylesFn(theme)), [theme, stylesFn]);
};

// 팀 컬러 훅
export const useTeamColors = () => {
  const { theme, teamName } = useTheme();
  return {
    colors: theme.colors.team,
    teamName,
  };
};

// 반응형 값 헬퍼
export const useResponsiveValue = <T,>(
  values: {
    sm?: T;
    md?: T;
    lg?: T;
  },
  defaultValue: T
): T => {
  // React Native에서는 실제 화면 크기에 따라 구현 필요
  // 여기서는 기본값 반환
  return values.md || defaultValue;
};

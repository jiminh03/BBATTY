// import React, { createContext, useContext, useMemo, ReactNode } from 'react';
// import { Team } from './teamTypes';

// interface ThemeContextValue {
//   currentTeam: Team | null;
//   setCurrentTeam: (team: Team) => void;
//   themeColor: string;
// }

// const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

// interface ThemeProviderProps {
//   children: ReactNode;
//   initialTeam?: Team | null;
// }

// export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children, initialTeam = null }) => {
//   const [currentTeam, setCurrentTeam] = React.useState<Team | null>(initialTeam);

//   const value = useMemo(
//     () => ({
//       currentTeam,
//       setCurrentTeam,
//       themeColor: currentTeam?.color || '#1D467F',
//     }),
//     [currentTeam]
//   );

//   return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
// };

// export const useTheme = () => {
//   const context = useContext(ThemeContext);
//   if (!context) {
//     throw new Error('useTheme must be used within a ThemeProvider');
//   }
//   return context;
// };

// export const useThemeColor = (): string => {
//   const { themeColor } = useTheme();
//   return themeColor;
// };

import React, { createContext, useContext, useMemo, useEffect, ReactNode } from 'react';
import { TEAMS, findTeamById, Team } from './teamTypes';

type TeamPalette = {
  primary: string;     // 팀 대표색
  onPrimary: string;   // 대표색 위 글자색
  background: string;  // 화면 배경
  surface: string;     // 카드/리스트 셀 배경
  text: string;        // 일반 텍스트
  tabActive: string;   // 탭 활성 텍스트/인디케이터
  tabInactive: string; // 탭 비활성 텍스트
  badgeBg: string;     // 뱃지/칩 배경
};

const DEFAULT_PRIMARY = '#1D467F'; // 기본(예: NC) — 팀 미선택 시
const DEFAULT_PALETTE: TeamPalette = {
  primary: DEFAULT_PRIMARY,
  onPrimary: '#FFFFFF',
  background: '#FFFFFF',
  surface: '#FFFFFF',
  text: '#111111',
  tabActive: '#111111',
  tabInactive: '#9AA2A9',
  badgeBg: '#F2F2F2',
};

function buildPalette(team?: Team | null): TeamPalette {
  const primary = team?.color ?? DEFAULT_PRIMARY;
  return { ...DEFAULT_PALETTE, primary };
}

interface ThemeContextValue {
  currentTeam: Team | null;
  setCurrentTeam: (team: Team | null) => void;
  setTeamById: (teamId: number) => void;
  themeColor: string;      // = palette.primary
  palette: TeamPalette;    // 팔레트 토큰
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

interface ThemeProviderProps {
  children: ReactNode;
  initialTeam?: Team | null;     // 초기 팀 객체로 세팅하고 싶을 때
  initialTeamId?: number | null; // 또는 teamId만 줄 때
}

/** 앱 전역 팀 테마 컨텍스트 */
export const ThemeProvider: React.FC<ThemeProviderProps> = ({
  children,
  initialTeam = null,
  initialTeamId = null,
}) => {
  const [currentTeam, setCurrentTeam] = React.useState<Team | null>(
    initialTeam ?? (initialTeamId ? findTeamById(initialTeamId) ?? null : null)
  );

  // initialTeamId가 바뀌면 동기화(선택적)
  useEffect(() => {
    if (initialTeamId != null) {
      const t = findTeamById(initialTeamId) ?? null;
      setCurrentTeam(t);
    }
  }, [initialTeamId]);

  const palette = useMemo(() => buildPalette(currentTeam), [currentTeam]);

  const value = useMemo<ThemeContextValue>(
    () => ({
      currentTeam,
      setCurrentTeam,
      setTeamById: (teamId: number) => {
        const t = findTeamById(teamId) ?? null;
        setCurrentTeam(t);
      },
      themeColor: palette.primary,
      palette,
    }),
    [currentTeam, palette]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useTheme = () => {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used within a ThemeProvider');
  return ctx;
};

export const useThemeColor = (): string => useTheme().themeColor;
export const useTeamPalette = (): TeamPalette => useTheme().palette;


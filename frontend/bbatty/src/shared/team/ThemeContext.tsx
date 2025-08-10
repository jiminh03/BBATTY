// shared/context/ThemeContext.tsx
import React, { createContext, useContext, useMemo, ReactNode } from 'react';
import { Team } from './teamTypes';

interface ThemeContextValue {
  currentTeam: Team | null;
  setCurrentTeam: (team: Team) => void;
  themeColor: string;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

interface ThemeProviderProps {
  children: ReactNode;
  initialTeam?: Team | null;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children, initialTeam = null }) => {
  const [currentTeam, setCurrentTeam] = React.useState<Team | null>(initialTeam);

  const value = useMemo(
    () => ({
      currentTeam,
      setCurrentTeam,
      themeColor: currentTeam?.color || '#1D467F',
    }),
    [currentTeam]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

export const useThemeColor = (): string => {
  const { themeColor } = useTheme();
  return themeColor;
};

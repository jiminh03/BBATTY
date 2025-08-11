export interface BasicStats {
  userId: number;
  season: string;
  wins: number;
  draws: number;
  losses: number;
  totalGames: number;
  winRate: string;
}

export interface StreakStats {
  userId: number;
  season: string;
  wins: number;
  draws: number;
  losses: number;
  totalGames: number;
  maxWinStreakCurrentSeason: number;
  currentSeason: string;
  maxWinStreakBySeason: Record<string, number>;
  maxWinStreakAll: number;
  currentWinStreak: number;
}

export interface StadiumStats {
  stadiumStats: Record<string, any>;
}

export interface OpponentStats {
  opponentStats: Record<string, any>;
}

export interface DayOfWeekStats {
  dayOfWeekStats: Record<string, any>;
}

export interface HomeAwayStats {
  homeStats: Record<string, any>;
  awayStats: Record<string, any>;
}

export type StatsType = 'basic' | 'streak' | 'stadium' | 'opponent' | 'dayOfWeek' | 'homeAway';

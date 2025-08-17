// features/user-profile/model/statsTypes.ts (통합된 파일)

// ================================= 기본 통계 =================================
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
  currentSeason: string;
  currentWinStreak: number;
  maxWinStreakAll: number;
  maxWinStreakCurrentSeason: number;
  maxWinStreakBySeason: Record<string, number>;
  totalGames: number;
  wins: number;
  draws: number;
  losses: number;
}

// ================================= 상세 통계 =================================
export interface HomeAwayStats {
  homeStats: {
    wins: number;
    draws: number;
    losses: number;
    games: number;
    winRate: string;
  };
  awayStats: {
    wins: number;
    draws: number;
    losses: number;
    games: number;
    winRate: string;
  };
}

export interface StadiumStats {
  stadiumStats: Record<
    string,
    {
      wins: number;
      draws: number;
      losses: number;
      games: number;
      winRate: string;
    }
  >;
}

export interface OpponentStats {
  opponentStats: Record<
    string,
    {
      wins: number;
      draws: number;
      losses: number;
      games: number;
      winRate: string;
    }
  >;
}

export interface DayOfWeekStats {
  dayOfWeekStats: Record<
    string,
    {
      wins: number;
      draws: number;
      losses: number;
      games: number;
      winRate: string;
    }
  >;
}

// ================================= UI용 통계 인터페이스 =================================
export interface TeamStats {
  teamId: number;
  teamName: string;
  matches: number;
  wins: number;
  draws: number;
  losses: number;
  winRate: number;
}

export interface StadiumStatsItem {
  stadiumId: number;
  stadiumName: string;
  matches: number;
  wins: number;
  winRate: number;
  location?: {
    latitude: number;
    longitude: number;
  };
}

export interface DayOfWeekStatsItem {
  dayName: string;
  matches: number;
  wins: number;
  winRate: number;
}

export interface WinRateStats {
  overall: number;
  home: number;
  away: number;
}

// ================================= 직관 기록 =================================
export interface DirectViewRecord {
  id: number;
  date: string;
  homeTeam: string;
  awayTeam: string;
  homeScore: number;
  awayScore: number;
  stadium: string;
  isWin: boolean;
  season: string;
}

// 새로운 직관 기록 API 응답 타입
export interface AttendanceRecord {
  gameId: number;
  homeTeam: string;
  awayTeam: string;
  homeScore?: number;
  awayScore?: number;
  dateTime: string;
  stadium: string;
  status: string;
}

export interface AttendanceRecordsResponse {
  nextCursor: number | null;
  records: AttendanceRecord[];
  hasMore: boolean;
}

// ================================= 종합 통계 =================================
export interface DetailedUserStats {
  badges: {
    visitedStadiums: number;
    totalMatches: number;
    totalWins: number;
  };
  winRates: WinRateStats;
  teamStats: TeamStats[];
  stadiumStats: StadiumStatsItem[];
  dayStats: DayOfWeekStatsItem[];
}

// ================================= 타입 정의 =================================
export type StatsType = 'basic' | 'streak' | 'stadium' | 'opponent' | 'dayOfWeek' | 'homeAway';

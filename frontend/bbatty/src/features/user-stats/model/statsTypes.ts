export interface UserBadges {
  visitedStadiums: number;
  totalMatches: number;
  totalWins: number;
}

export interface WinRateStats {
  overall: number;
  home: number;
  away: number;
}

export interface TeamStats {
  teamId: number;
  teamName: string;
  matches: number;
  wins: number;
  winRate: number;
}

export interface StadiumStats {
  stadiumId: number;
  stadiumName: string;
  matches: number;
  wins: number;
  winRate: number;
  location: {
    latitude: number;
    longitude: number;
  };
}

export interface DayOfWeekStats {
  dayName: string;
  matches: number;
  wins: number;
  winRate: number;
}

export interface DetailedUserStats {
  badges: UserBadges;
  winRates: WinRateStats;
  teamStats: TeamStats[];
  stadiumStats: StadiumStats[];
  dayStats: DayOfWeekStats[];
}

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

export type Season = '2024' | '2023' | '2022' | '전체';

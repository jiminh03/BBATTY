export interface Game {
  gameId: number;
  awayTeamId: number;
  homeTeamId: number;
  awayTeamName: string;
  homeTeamName: string;
  status: 'SCHEDULED' | 'LIVE' | 'FINISHED' | 'POSTPONED' | 'CANCELLED';
  dateTime: string;
  stadium: string;
  activeUserCount: number;
}

export interface GamesByDate {
  date: string;
  games: Game[];
}

export interface GamesResponse {
  status: string;
  message: string;
  data: GamesByDate[];
}
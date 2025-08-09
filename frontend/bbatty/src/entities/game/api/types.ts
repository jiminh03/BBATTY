export interface Game {
  gameId: number;
  awayTeamName: string;
  homeTeamName: string;
  dateTime: string;
  stadium: string;
}

export interface GamesResponse {
  status: string;
  message: string;
  data: Game[];
}
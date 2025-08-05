export interface AuthResponse {
  success: boolean;
  sessionToken: string;
  websocketUrl: string;
  errorMessage?: string;
}

export interface MatchChatJoinRequest {
  matchId: string;
  nickname: string;
  winRate: number;
  profileImgUrl: string;
  isVictoryFairy: boolean;
}

export interface WatchChatJoinRequest {
  gameId: string;
  isAttendanceVerified: boolean;
}

export type AuthRequest = MatchChatJoinRequest | WatchChatJoinRequest;
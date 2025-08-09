export interface AuthResponse {
  sessionToken: string;
  websocketUrl: string;
}

export interface MatchChatJoinRequest {
  matchId: string;
  nickname: string;
  winRate: number;
  profileImgUrl: string;
  isWinFairy: boolean;
}

export interface WatchChatJoinRequest {
  gameId: number;
  teamId: number;
  isAttendanceVerified: boolean;
}

export type AuthRequest = MatchChatJoinRequest | WatchChatJoinRequest;

export interface MatchChatRoom {
  matchId: string;
  gameId: string | null;
  matchTitle: string;
  matchDescription: string;
  teamId: string;
  minAge: number;
  maxAge: number;
  genderCondition: 'ALL' | 'MALE' | 'FEMALE';
  maxParticipants: number;
  currentParticipants: number;
  createdAt: string;
  status: 'ACTIVE' | 'INACTIVE';
  websocketUrl: string;
}

export interface MatchChatRoomsResponse {
  status: string;
  message: string;
  data: {
    rooms: MatchChatRoom[];
    nextCursor: string | null;
    hasMore: boolean;
    count: number;
  };
}

export interface CreateMatchChatRoomRequest {
  gameId: string;
  matchTitle: string;
  matchDescription: string;
  teamId: string;
  minAge: number;
  maxAge: number;
  genderCondition: 'ALL' | 'MALE' | 'FEMALE';
  maxParticipants: number;
  nickname: string;
}

export interface CreateMatchChatRoomResponse {
  status: string;
  message: string;
  data: MatchChatRoom;
}
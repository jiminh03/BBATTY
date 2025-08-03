export type ChatType = 'WATCH' | 'MATCH';

export type ChatRoomType = 'game' | 'match';

export interface ChatRoom {
  id: string;
  type: ChatRoomType;
  chatType: ChatType;
  isActive: boolean;
  participants: number;
  createdAt: string;
  updatedAt: string;
}

export interface GameChatRoom extends ChatRoom {
  type: 'game';
  chatType: 'WATCH';
  teamId: string;
  gameId: string;
  userTeamId: string;
}

export interface MatchChatRoom extends ChatRoom {
  type: 'match';
  chatType: 'MATCH';
  matchId: string;
}
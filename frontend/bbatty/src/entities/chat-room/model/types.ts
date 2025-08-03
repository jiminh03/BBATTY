import { ChatRoomType } from '../../../shared/types/enums';

export interface WatchChatRoom {
  roomId: string;
  gameId: string;
  teamId: string;
  type: ChatRoomType.GAME;
}

export interface MatchChatRoom {
  roomId: string;
  matchChatRoomId: string;
  type: ChatRoomType.MATCH;
}

export type ChatRoom = WatchChatRoom | MatchChatRoom;

export interface AuthRequest {
  chatType: string;
  roomId: string;
}

export interface AuthResponse {
  success: boolean;
  sessionToken?: string;
  errorMessage?: string;
}

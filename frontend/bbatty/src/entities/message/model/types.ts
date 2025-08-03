export type MessageType = 
  | 'CHAT' 
  | 'SYSTEM' 
  | 'ERROR' 
  | 'USER_JOIN' 
  | 'USER_LEAVE' 
  | 'USER_ACTIVITY';

export interface BaseMessage {
  type: string;
  messageId?: string;
  content: string;
  timestamp: string;
  messageType: MessageType;
}

// 게임 채팅 메시지 (익명)
export interface GameChatMessage extends BaseMessage {
  type: 'message';
  roomId: string;
  messageType: 'CHAT';
  chatType: 'game';
  teamId: string;
  serverId?: string;
  messageSequence?: number;
}

// 매칭 채팅 메시지
export interface MatchChatMessage extends BaseMessage {
  type: 'message';
  roomId: string;
  userId: string;  
  nickname: string;
  messageType: 'CHAT';
  profileImgUrl?: string;
  isVictoryFairy?: boolean;
}

// 시스템 메시지
export interface SystemMessage extends BaseMessage {
  type: 'user_join' | 'user_leave' | 'system' | 'error';
  userId?: string;
  userName?: string;
  teamId?: string;
  matchId?: string;
  serverId?: string;
}

export type ChatMessage = GameChatMessage | MatchChatMessage | SystemMessage;
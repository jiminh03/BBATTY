export type MessageType = 'CHAT' | 'SYSTEM' | 'ERROR' | 'USER_JOIN' | 'USER_LEAVE' | 'USER_ACTIVITY';

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

// 히스토리 로드 요청
export interface LoadMoreMessagesRequest {
  type: 'LOAD_MORE_MESSAGES';
  matchId: string;
  lastMessageTimestamp: number;
  limit: number;
}

// 히스토리 로드 응답
export interface LoadMoreMessagesResponse {
  type: 'LOAD_MORE_MESSAGES_RESPONSE';
  matchId: string;
  messages: ChatMessage[];
  hasMore: boolean;
  totalCount: number;
}

// WebSocket 메시지 타입
export interface WebSocketMessage {
  type: string;
  payload?: any;
  messageId?: string;
  timestamp?: string;
}

export type ChatMessage = GameChatMessage | MatchChatMessage | SystemMessage;

// 메시지 수신 관련 설정
export interface ReceiveMessageConfig {
  roomId: string;
  maxMessages?: number;
  enableAutoScroll?: boolean;
  loadMoreLimit?: number;
}

// 메시지 필터링 옵션
export interface MessageFilterOptions {
  messageTypes?: MessageType[];
  userId?: string;
  startTime?: Date;
  endTime?: Date;
}
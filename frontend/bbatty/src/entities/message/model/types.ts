import { MessageType } from '../../../shared/types/enums';

export interface BaseMessage {
  roomId: string;
  content: string;
  timestamp?: string;
  type?: MessageType;
}

export interface WatchMessage extends BaseMessage {
  // Watch 채팅은 기본 필드만 사용
}

export interface MatchMessage extends BaseMessage {
  userId: string;
  nickname: string;
  profileImgUrl?: string;
  isVictoryFairy?: boolean;
}

export type ChatMessage = WatchMessage | MatchMessage;
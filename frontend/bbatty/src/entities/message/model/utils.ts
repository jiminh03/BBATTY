import { ChatMessage, MatchMessage, WatchMessage } from './types';
import { MessageType } from '../../../shared/types/enums';

export const isMatchMessage = (message: ChatMessage): message is MatchMessage => {
  return 'userId' in message && 'nickname' in message;
};

export const isWatchMessage = (message: ChatMessage): message is WatchMessage => {
  return !isMatchMessage(message);
};

export const createWatchMessage = (roomId: string, content: string): WatchMessage => {
  return {
    roomId,
    content,
    timestamp: new Date().toISOString(),
    type: MessageType.CHAT,
  };
};

export const createMatchMessage = (
  roomId: string,
  content: string,
  userId: string,
  nickname: string,
  profileImgUrl?: string,
  isVictoryFairy?: boolean
): MatchMessage => {
  return {
    roomId,
    content,
    userId,
    nickname,
    profileImgUrl,
    isVictoryFairy,
    timestamp: new Date().toISOString(),
    type: MessageType.CHAT,
  };
};

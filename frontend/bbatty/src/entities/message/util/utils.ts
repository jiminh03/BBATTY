import { ChatMessage, MessageType, GameChatMessage, MatchChatMessage, SystemMessage } from '../model/types';

export const messageUtils = {
  // 메시지 타입 체크
  isGameMessage: (message: ChatMessage): message is GameChatMessage => {
    return 'chatType' in message && message.chatType === 'game';
  },

  isMatchMessage: (message: ChatMessage): message is MatchChatMessage => {
    return 'userId' in message && 'nickname' in message;
  },

  isSystemMessage: (message: ChatMessage): message is SystemMessage => {
    return ['user_join', 'user_leave', 'system', 'error'].includes(message.type);
  },

  // 메시지 유효성 검증
  isValidMessage: (content: string): boolean => {
    if (!content || content.trim().length === 0) return false;
    if (content.length > 500) return false;
    return true;
  },

  // 메시지 ID 생성
  generateMessageId: (): string => {
    return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  },

  // 타임스탬프 포맷팅
  formatTimestamp: (timestamp: string): string => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('ko-KR', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  },

  // 메시지 정렬 (시간순)
  sortMessages: (messages: ChatMessage[]): ChatMessage[] => {
    return [...messages].sort((a, b) => 
      new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );
  },
};
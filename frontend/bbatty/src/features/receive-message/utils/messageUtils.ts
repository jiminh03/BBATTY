import { ChatMessage, GameChatMessage, MatchChatMessage, SystemMessage } from '../model/types';

/**
 * 시스템 메시지인지 확인 (타입 가드)
 */
export function isSystemMessage(message: ChatMessage): message is SystemMessage {
  return (
    message.messageType === 'SYSTEM' ||
    message.messageType === 'USER_JOIN' ||
    message.messageType === 'USER_LEAVE' ||
    message.messageType === 'ERROR'
  );
}

/**
 * 매칭 채팅 메시지인지 확인 (타입 가드)
 */
export function isMatchChatMessage(message: ChatMessage): message is MatchChatMessage {
  return message.messageType === 'CHAT' && 'nickname' in message;
}

/**
 * 게임 채팅 메시지인지 확인 (타입 가드)
 */
export function isGameChatMessage(message: ChatMessage): message is GameChatMessage {
  return message.messageType === 'CHAT' && 'teamId' in message && !('nickname' in message);
}

export const messageUtils = {
  isSystemMessage,
  isMatchChatMessage,
  isGameChatMessage,

  formatTime: (timestamp: string): string => {
    const date = new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  formatMessageTime: (timestamp: string): string => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffInDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));

    if (diffInDays === 0) return messageUtils.formatTime(timestamp);
    if (diffInDays === 1) return `어제 ${messageUtils.formatTime(timestamp)}`;
    if (diffInDays < 7) {
      const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
      return `${weekdays[date.getDay()]}요일 ${messageUtils.formatTime(timestamp)}`;
    }

    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return `${month}.${day} ${messageUtils.formatTime(timestamp)}`;
  },

  isMyMessage: (message: ChatMessage, currentUserId?: string): boolean => {
    if (!currentUserId) return false;
    return messageUtils.isMatchChatMessage(message) && message.userId === currentUserId;
  },

  getTimeDifferenceInMinutes: (timestamp1: string, timestamp2: string): number => {
    const date1 = new Date(timestamp1);
    const date2 = new Date(timestamp2);
    return Math.abs(date1.getTime() - date2.getTime()) / (1000 * 60);
  },

  shouldGroupMessages: (prev: ChatMessage, current: ChatMessage): boolean => {
    if (prev.type !== current.type) return false;
    if (messageUtils.isSystemMessage(prev) || messageUtils.isSystemMessage(current)) return false;
    if (messageUtils.getTimeDifferenceInMinutes(prev.timestamp, current.timestamp) > 5) return false;
    if (
      messageUtils.isMatchChatMessage(prev) &&
      messageUtils.isMatchChatMessage(current) &&
      prev.userId === current.userId
    ) {
      return true;
    }
    return false;
  },

  sanitizeMessageContent: (content: string): string => {
    return content.trim().replace(/\s+/g, ' ').substring(0, 1000);
  },

  searchMessages: (messages: ChatMessage[], query: string): ChatMessage[] => {
    if (!query.trim()) return messages;
    const lower = query.toLowerCase();
    return messages.filter(msg => {
      if (msg.content.toLowerCase().includes(lower)) return true;
      if (messageUtils.isMatchChatMessage(msg)) {
        return msg.nickname.toLowerCase().includes(lower);
      }
      return false;
    });
  },

  sortMessagesByTimestamp: (messages: ChatMessage[], ascending = true): ChatMessage[] => {
    return [...messages].sort((a, b) =>
      ascending
        ? new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
        : new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );
  },

  removeDuplicateMessages: (messages: ChatMessage[]): ChatMessage[] => {
    const seen = new Set<string>();
    return messages.filter(msg => {
      const key = msg.messageId || `${msg.timestamp}-${msg.content}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  },

  getMessageStats: (messages: ChatMessage[]) => {
    const total = messages.length;
    const chatMessages = messages.filter(msg => msg.messageType === 'CHAT').length;
    const systemMessages = messages.filter(msg => msg.messageType === 'SYSTEM').length;

    const userStats: Record<string, number> = {};
    messages.forEach(msg => {
      if (messageUtils.isMatchChatMessage(msg)) {
        userStats[msg.userId] = (userStats[msg.userId] || 0) + 1;
      }
    });

    const mostActiveUser = Object.entries(userStats).reduce((max, curr) =>
      curr[1] > max[1] ? curr : max, ['', 0]
    )[0] || null;

    return {
      total,
      chatMessages,
      systemMessages,
      userStats,
      mostActiveUser
    };
  },

  exportMessagesToText: (messages: ChatMessage[]): string => {
  return messages.map((message) => {
    const time = messageUtils.formatMessageTime(message.timestamp);

    if (messageUtils.isSystemMessage(message)) {
      return `[${time}] ${message.content}`;
    }

    if (messageUtils.isMatchChatMessage(message)) {
      return `[${time}] ${message.nickname}: ${message.content}`;
    }

    if (messageUtils.isGameChatMessage(message)) {
      return `[${time}] 익명: ${message.content}`;
    }

    // 타입 안전성을 위해 fallback 처리
    return `[${time}] 알 수 없는 메시지 유형`;
  }).join('\n');
}
};

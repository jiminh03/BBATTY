import { messageValidation } from "./validation";

export const messageFormatter = {
  // 메시지 포맷팅
  formatForSend: (content: string, roomId: string): {
    content: string;
    roomId: string;
    timestamp: string;
    messageId: string;
  } => {
    return {
      content: messageValidation.sanitizeMessage(content),
      roomId,
      timestamp: new Date().toISOString(),
      messageId: `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    };
  },

  // 특수 명령어 처리
  parseCommand: (content: string): { isCommand: boolean; command?: string; args?: string[] } => {
    if (!content.startsWith('/')) {
      return { isCommand: false };
    }

    const parts = content.slice(1).split(' ');
    const command = parts[0].toLowerCase();
    const args = parts.slice(1);

    return {
      isCommand: true,
      command,
      args,
    };
  },

  // 이모지 변환
  processEmojis: (content: string): string => {
    const emojiMap = {
      ':)': '😊',
      ':D': '😄',
      ':(': '😢',
      ':P': '😛',
      '<3': '❤️',
    };

    let processed = content;
    Object.entries(emojiMap).forEach(([text, emoji]) => {
      processed = processed.replace(new RegExp(text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), emoji);
    });

    return processed;
  },
};
import { create } from 'zustand';
import { ChatMessage } from './types';

interface ReceiveMessageState {
  // 메시지 관련
  messagesByRoom: Record<string, ChatMessage[]>;
  isLoadingMessages: boolean;
  hasMoreMessages: Record<string, boolean>;
  lastMessageTimestamp: Record<string, number>;
  
  // 연결 상태
  isReceiving: boolean;
  connectionError: string | null;
}

interface ReceiveMessageActions {
  // 메시지 관리
  addMessage: (roomId: string, message: ChatMessage) => void;
  addMessages: (roomId: string, messages: ChatMessage[], prepend?: boolean) => void;
  clearMessages: (roomId: string) => void;
  clearAllMessages: () => void;
  
  // 상태 관리
  setLoadingMessages: (isLoading: boolean) => void;
  setHasMoreMessages: (roomId: string, hasMore: boolean) => void;
  setLastMessageTimestamp: (roomId: string, timestamp: number) => void;
  setReceiving: (isReceiving: boolean) => void;
  setConnectionError: (error: string | null) => void;
  
  // 유틸리티
  getMessagesByRoom: (roomId: string) => ChatMessage[];
  getLastMessageTimestamp: (roomId: string) => number | null;
  hasMoreMessagesForRoom: (roomId: string) => boolean;
}

type ReceiveMessageStore = ReceiveMessageState & ReceiveMessageActions;

const MAX_MESSAGES_PER_ROOM = 200; // 방별 최대 메시지 수

export const useReceiveMessageStore = create<ReceiveMessageStore>((set, get) => ({
  // State
  messagesByRoom: {},
  isLoadingMessages: false,
  hasMoreMessages: {},
  lastMessageTimestamp: {},
  isReceiving: false,
  connectionError: null,

  // Actions
  addMessage: (roomId, message) => set((state) => {
    const existingMessages = state.messagesByRoom[roomId] || [];
    
    // 중복 메시지 체크 (messageId 기준)
    if (message.messageId && existingMessages.some(msg => msg.messageId === message.messageId)) {
      return state;
    }
    
    const newMessages = [...existingMessages, message];
    
    // 최대 메시지 수 제한
    if (newMessages.length > MAX_MESSAGES_PER_ROOM) {
      newMessages.splice(0, newMessages.length - MAX_MESSAGES_PER_ROOM);
    }
    
    // 최신 메시지 타임스탬프 업데이트
    const messageTime = new Date(message.timestamp).getTime();
    const currentLastTime = state.lastMessageTimestamp[roomId] || 0;
    
    return {
      messagesByRoom: {
        ...state.messagesByRoom,
        [roomId]: newMessages
      },
      lastMessageTimestamp: {
        ...state.lastMessageTimestamp,
        [roomId]: Math.max(currentLastTime, messageTime)
      }
    };
  }),

  addMessages: (roomId, messages, prepend = false) => set((state) => {
    const existingMessages = state.messagesByRoom[roomId] || [];
    
    // 중복 제거
    const filteredMessages = messages.filter(newMsg => 
      !newMsg.messageId || !existingMessages.some(existingMsg => 
        existingMsg.messageId === newMsg.messageId
      )
    );
    
    if (filteredMessages.length === 0) {
      return state;
    }
    
    // 메시지 정렬 (타임스탬프 기준)
    const sortedFilteredMessages = filteredMessages.sort((a, b) => 
      new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );
    
    let newMessages: ChatMessage[];
    if (prepend) {
      // 이전 메시지들을 앞에 추가 (더보기)
      newMessages = [...sortedFilteredMessages, ...existingMessages];
    } else {
      // 새 메시지들을 뒤에 추가
      newMessages = [...existingMessages, ...sortedFilteredMessages];
    }
    
    // 최대 메시지 수 제한
    if (newMessages.length > MAX_MESSAGES_PER_ROOM) {
      if (prepend) {
        // 이전 메시지 추가 시에는 뒤에서 자르기
        newMessages = newMessages.slice(0, MAX_MESSAGES_PER_ROOM);
      } else {
        // 일반적인 경우에는 앞에서 자르기
        newMessages = newMessages.slice(-MAX_MESSAGES_PER_ROOM);
      }
    }
    
    // 타임스탬프 업데이트
    const timestamps = filteredMessages.map(msg => new Date(msg.timestamp).getTime());
    const maxTimestamp = Math.max(...timestamps);
    const currentLastTime = state.lastMessageTimestamp[roomId] || 0;
    
    return {
      messagesByRoom: {
        ...state.messagesByRoom,
        [roomId]: newMessages
      },
      lastMessageTimestamp: {
        ...state.lastMessageTimestamp,
        [roomId]: Math.max(currentLastTime, maxTimestamp)
      }
    };
  }),

  clearMessages: (roomId) => set((state) => {
    const newMessagesByRoom = { ...state.messagesByRoom };
    const newHasMoreMessages = { ...state.hasMoreMessages };
    const newLastMessageTimestamp = { ...state.lastMessageTimestamp };
    
    delete newMessagesByRoom[roomId];
    delete newHasMoreMessages[roomId];
    delete newLastMessageTimestamp[roomId];
    
    return {
      messagesByRoom: newMessagesByRoom,
      hasMoreMessages: newHasMoreMessages,
      lastMessageTimestamp: newLastMessageTimestamp
    };
  }),

  clearAllMessages: () => set({
    messagesByRoom: {},
    hasMoreMessages: {},
    lastMessageTimestamp: {}
  }),

  setLoadingMessages: (isLoading) => set({ isLoadingMessages: isLoading }),

  setHasMoreMessages: (roomId, hasMore) => set((state) => ({
    hasMoreMessages: {
      ...state.hasMoreMessages,
      [roomId]: hasMore
    }
  })),

  setLastMessageTimestamp: (roomId, timestamp) => set((state) => ({
    lastMessageTimestamp: {
      ...state.lastMessageTimestamp,
      [roomId]: timestamp
    }
  })),

  setReceiving: (isReceiving) => set({ isReceiving }),

  setConnectionError: (error) => set({ connectionError: error }),

  // Getters
  getMessagesByRoom: (roomId) => {
    const { messagesByRoom } = get();
    return messagesByRoom[roomId] || [];
  },

  getLastMessageTimestamp: (roomId) => {
    const { lastMessageTimestamp } = get();
    return lastMessageTimestamp[roomId] || null;
  },

  hasMoreMessagesForRoom: (roomId) => {
    const { hasMoreMessages } = get();
    return hasMoreMessages[roomId] ?? true; // 기본값은 true
  },
}));
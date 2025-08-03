import { create } from 'zustand';
import { ChatMessage } from './types';

interface MessageStore {
  messages: Record<string, ChatMessage[]>; // roomId별로 메시지 저장
  addMessage: (roomId: string, message: ChatMessage) => void;
  clearMessages: (roomId: string) => void;
  getMessages: (roomId: string) => ChatMessage[];
}

export const useMessageStore = create<MessageStore>((set, get) => ({
  messages: {},
  
  addMessage: (roomId: string, message: ChatMessage) => {
    set((state) => ({
      messages: {
        ...state.messages,
        [roomId]: [...(state.messages[roomId] || []), message]
      }
    }));
  },
  
  clearMessages: (roomId: string) => {
    set((state) => ({
      messages: {
        ...state.messages,
        [roomId]: []
      }
    }));
  },
  
  getMessages: (roomId: string) => {
    return get().messages[roomId] || [];
  }
}));
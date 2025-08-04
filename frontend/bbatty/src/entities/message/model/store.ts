import { create } from 'zustand';
import { ChatMessage } from './types';

interface MessageState {
  messages: ChatMessage[];
  isLoading: boolean;
}

interface MessageActions {
  addMessage: (message: ChatMessage) => void;
  addMessages: (messages: ChatMessage[]) => void;
  clearMessages: () => void;
  setLoading: (loading: boolean) => void;
  getMessagesByRoom: (roomId: string) => ChatMessage[];
}

type MessageStore = MessageState & MessageActions;

const MAX_MESSAGES = 100; // 최대 메시지 수

export const useMessageStore = create<MessageStore>((set, get) => ({
  // State
  messages: [],
  isLoading: false,

  // Actions
  addMessage: (message) => set((state) => {
    const newMessages = [...state.messages, message];
    // 최대 메시지 수 제한
    if (newMessages.length > MAX_MESSAGES) {
      newMessages.splice(0, newMessages.length - MAX_MESSAGES);
    }
    return { messages: newMessages };
  }),

  addMessages: (messages) => set((state) => {
    const newMessages = [...state.messages, ...messages];
    // 최대 메시지 수 제한
    if (newMessages.length > MAX_MESSAGES) {
      newMessages.splice(0, newMessages.length - MAX_MESSAGES);
    }
    return { messages: newMessages };
  }),

  clearMessages: () => set({ messages: [] }),

  setLoading: (loading) => set({ isLoading: loading }),

  getMessagesByRoom: (roomId) => {
    const { messages } = get();
    return messages.filter(message => 
      'roomId' in message && message.roomId === roomId
    );
  },
}));
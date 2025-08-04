import { create } from 'zustand';
import { ChatRoom, ChatType } from './types';

interface ChatRoomState {
  currentRoom: ChatRoom | null;
  sessionToken: string | null;
  isConnected: boolean;
  connectionStatus: 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'AUTHENTICATING' | 'CLOSING' | 'CLOSED';
}

interface ChatRoomActions {
  setCurrentRoom: (room: ChatRoom | null) => void;
  setSessionToken: (token: string | null) => void;
  setConnectionStatus: (status: ChatRoomState['connectionStatus']) => void;
  setConnected: (connected: boolean) => void;
  reset: () => void;
}

type ChatRoomStore = ChatRoomState & ChatRoomActions;

export const useChatRoomStore = create<ChatRoomStore>((set) => ({
  // State
  currentRoom: null,
  sessionToken: null,
  isConnected: false,
  connectionStatus: 'DISCONNECTED',

  // Actions
  setCurrentRoom: (room) => set({ currentRoom: room }),
  setSessionToken: (token) => set({ sessionToken: token }),
  setConnectionStatus: (status) => {
    set({ 
      connectionStatus: status,
      isConnected: status === 'CONNECTED'
    });
  },
  setConnected: (connected) => set({ isConnected: connected }),
  reset: () => set({
    currentRoom: null,
    sessionToken: null,
    isConnected: false,
    connectionStatus: 'DISCONNECTED',
  }),
}));
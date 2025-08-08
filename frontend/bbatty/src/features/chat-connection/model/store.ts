import { create } from 'zustand';
import { SocketClient } from '../../../shared/api/lib/socket/socket-client';

interface ConnectionState {
  client: SocketClient | null;
  isConnecting: boolean;
  isConnected: boolean;
  connectionStatus: 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING' | 'FAILED' | 'ERROR';
  error: string | null;
  reconnectAttempts: number;
  lastConnectedAt: string | null;
}

interface ConnectionActions {
  setClient: (client: SocketClient | null) => void;
  setConnecting: (connecting: boolean) => void;
  setConnected: (connected: boolean) => void;
  setConnectionStatus: (status: ConnectionState['connectionStatus']) => void;
  setError: (error: string | null) => void;
  incrementReconnectAttempts: () => void;
  resetReconnectAttempts: () => void;
  setReconnectAttempts: (attempts: number) => void;
  setLastConnectedAt: (timestamp: string) => void;
  reset: () => void;
}

type ConnectionStore = ConnectionState & ConnectionActions;

export const useConnectionStore = create<ConnectionStore>((set) => ({
  // State
  client: null,
  isConnecting: false,
  isConnected: false,
  connectionStatus: 'DISCONNECTED',
  error: null,
  reconnectAttempts: 0,
  lastConnectedAt: null,

  // Actions
  setClient: (client) => set({ client }),
  setConnecting: (connecting) => set({ isConnecting: connecting }),
  setConnected: (connected) => set({ isConnected: connected }),
  setConnectionStatus: (status) => set({ 
    connectionStatus: status,
    isConnected: status === 'CONNECTED'
  }),
  setError: (error) => set({ error }),
  incrementReconnectAttempts: () => set((state) => ({ 
    reconnectAttempts: state.reconnectAttempts + 1 
  })),
  resetReconnectAttempts: () => set({ reconnectAttempts: 0 }),
  setReconnectAttempts: (attempts) => set({ reconnectAttempts: attempts }),
  setLastConnectedAt: (timestamp) => set({ lastConnectedAt: timestamp }),
  reset: () => set({
    client: null,
    isConnecting: false,
    isConnected: false,
    connectionStatus: 'DISCONNECTED',
    error: null,
    reconnectAttempts: 0,
    lastConnectedAt: null,
  }),
}));
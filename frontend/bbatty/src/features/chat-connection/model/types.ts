export interface ConnectionConfig {
  url: string;
  options?: {
    auth?: { 
      sessionToken?: string;
      userId?: string;
    };
    query?: Record<string, any>;
    timeout?: number;
    reconnect?: boolean;
  };
}

export type ConnectionStatus = 
  | 'DISCONNECTED' 
  | 'CONNECTING' 
  | 'CONNECTED' 
  | 'RECONNECTING' 
  | 'ERROR';

export interface SocketClient {
  connect(): Promise<void>;
  disconnect(): void;
  getConnectionStatus(): boolean;
  on(event: string, callback: Function): void;
  emit(event: string, data?: any): void;
  sendChatMessage(content: string): void;
  joinRoom(roomId: string, userData?: any): void;
  leaveRoom(roomId: string): void;
}
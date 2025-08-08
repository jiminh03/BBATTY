export interface ConnectionConfig {
  url: string;
  sessionToken?: string;
  userId?: string;
  teamId?: string;
  roomId?: string;
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
  | 'FAILED'
  | 'ERROR';

export interface ConnectionEvents {
  onConnect?: () => void;
  onDisconnect?: (reason: string) => void;
  onError?: (error: any) => void;
  onMessage?: (message: any) => void;
  onReconnecting?: (data: any) => void;
  onMaxReconnectFailed?: (error: any) => void;
  onMessageSendError?: (error: any) => void;
}

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
export interface ConnectionConfig {
  url: string;
  sessionToken: string;
  roomId: string;
  userId: string;
  teamId?: string;
}

export interface ConnectionEvents {
  onConnect?: () => void;
  onDisconnect?: (reason: string) => void;
  onError?: (error: Error) => void;
  onMessage?: (message: any) => void;
  onReconnect?: () => void;
  onMaxReconnectFailed?: () => void;
}

export interface ConnectionMetrics {
  connectedAt: string | null;
  disconnectedAt: string | null;
  reconnectAttempts: number;
  totalMessages: number;
  lastMessageAt: string | null;
}
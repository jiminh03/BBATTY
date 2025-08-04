export interface SocketConfig {
  url: string;
  options?: {
    auth?: {
      token?: string;
      sessionToken?: string;
    };
    query?: Record<string, any>;
    headers?: Record<string, string>;
    [key: string]: any;
  };
}

export interface ChatAuthRequest {
  chatType: 'WATCH' | 'MATCH';
  roomId: string;
  userId: string;
  // 추가 인증 정보
}

export interface WebSocketMessage {
  type: string;
  payload: any;
  messageId?: string;
  timestamp?: string;
}
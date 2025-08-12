export interface BaseMessage {
  messageType: string;
  roomId: string;
  content: string;
  timestamp: string;
}

export interface MatchChatMessage extends BaseMessage {
  messageType: 'CHAT';
  userId: string;
  nickname: string;
  profileImgUrl?: string;
  winFair?: boolean;
}

export interface SystemMessage extends BaseMessage {
  type: 'user_join' | 'user_leave';
  userId?: string;
  userName?: string;
  messageType: 'USER_JOIN' | 'USER_LEAVE';
  kafkaTimestamp?: number;
}

export type ChatMessage = MatchChatMessage | SystemMessage;

export type ConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

// 메시지 전송 상태 타입들
export type MessageStatus = 'sending' | 'sent' | 'failed' | 'retrying';

export interface PendingMessage {
  id: string;
  content: string;
  timestamp: string;
  status: MessageStatus;
  retryCount: number;
  maxRetries: number;
  originalMessage: string;
}

export interface MessageWithStatus {
  messageType: string;
  roomId: string;
  content: string;
  timestamp: string;
  userId?: string;
  nickname?: string;
  profileImgUrl?: string;
  winFair?: boolean;
  type?: 'user_join' | 'user_leave';
  userName?: string;
  kafkaTimestamp?: number;
  id?: string;
  status?: MessageStatus;
  retryCount?: number;
  _isMyMessage?: boolean;
}

// 연결 상태 확장
export type ExtendedConnectionStatus = ConnectionStatus | 'RECONNECTING' | 'OFFLINE';

// 사용자 알림을 위한 인터페이스
export interface ChatNotification {
  id: string;
  type: 'info' | 'warning' | 'error' | 'success';
  message: string;
  duration?: number; // 자동 사라지는 시간 (ms)
  action?: {
    label: string;
    onPress: () => void;
  };
}
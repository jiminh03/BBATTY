import { SocketClient } from '../../../shared/api/lib/socket/socket-client';
import { useConnectionStore } from '../model/store';
import { ConnectionConfig, ConnectionEvents } from '../model/types';
import { useChatRoomStore } from '../../../entities/chat-room';
import { useMessageStore } from '../../../entities/message';
import { useUserStore } from '../../../entities/user';
import { getErrorMessage, logChatError, ChatError } from '../../../shared/utils/error';

export class ChatConnectionManager {
  private client: SocketClient | null = null;
  private config: ConnectionConfig | null = null;
  private events: ConnectionEvents = {};
  private isDestroyed = false;

  constructor(config: ConnectionConfig, events: ConnectionEvents = {}) {
    this.config = config;
    this.events = events;
    this.setupClient();
  }

  private setupClient() {
    if (!this.config) return;

    this.client = new SocketClient({
      url: this.config.url,
      options: {
        auth: { sessionToken: this.config.sessionToken },
        query: { 
          userId: this.config.userId, 
          teamId: this.config.teamId || '1',
          roomId: this.config.roomId
        }
      }
    });

    this.setupEventListeners();
    useConnectionStore.getState().setClient(this.client);
  }

  private setupEventListeners() {
    if (!this.client) return;

    // 연결 성공
    this.client.on('connect', () => {
      const now = new Date().toISOString();
      useConnectionStore.getState().setConnectionStatus('CONNECTED');
      useConnectionStore.getState().setLastConnectedAt(now);
      useConnectionStore.getState().resetReconnectAttempts();
      useConnectionStore.getState().setError(null);
      
      this.events.onConnect?.();
    });

    // 연결 해제
    this.client.on('disconnect', (data) => {
      useConnectionStore.getState().setConnectionStatus('DISCONNECTED');
      const reason = data?.reason || 'Unknown reason';
      this.events.onDisconnect?.(reason);
    });

    // 연결 오류
    this.client.on('connect_error', (chatError: ChatError) => {
      useConnectionStore.getState().setConnectionStatus('DISCONNECTED');
      useConnectionStore.getState().setError(chatError.userMessage);
      logChatError(chatError, { managerId: this.config?.userId });
      this.events.onError?.(chatError);
    });

    // 메시지 수신
    this.client.on('message', (message) => {
      // 메시지를 entities/message store에 추가
      if (message.type === 'message' || message.type === 'chat') {
        useMessageStore.getState().addMessage({
          ...message,
          messageId: message.messageId || `msg_${Date.now()}`,
          timestamp: message.timestamp || new Date().toISOString(),
        });
      }
      
      this.events.onMessage?.(message);
    });

    // 사용자 입장
    this.client.on('user_join', (data) => {
      if (data.userId && data.roomId) {
        useUserStore.getState().addUserToRoom(data.roomId, {
          id: data.userId,
          nickname: data.userName || '익명',
          status: 'ACTIVE',
          teamId: data.teamId || '',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        });
      }
    });

    // 사용자 퇴장
    this.client.on('user_leave', (data) => {
      if (data.userId && data.roomId) {
        useUserStore.getState().removeUserFromRoom(data.roomId, data.userId);
      }
    });

    // 재연결 시도 중
    this.client.on('reconnecting', (data) => {
      useConnectionStore.getState().setConnectionStatus('RECONNECTING');
      useConnectionStore.getState().setReconnectAttempts(data.attempt);
      const statusMessage = `재연결 중... (${data.attempt}/${data.maxAttempts})`;
      useConnectionStore.getState().setError(statusMessage);
      this.events.onReconnecting?.(data);
    });

    // 재연결 실패
    this.client.on('max_reconnect_failed', (chatError: ChatError) => {
      useConnectionStore.getState().setConnectionStatus('FAILED');
      useConnectionStore.getState().setError(chatError.userMessage);
      logChatError(chatError, { managerId: this.config?.userId });
      this.events.onMaxReconnectFailed?.(chatError);
    });

    // 메시지 전송 에러
    this.client.on('message_send_error', (chatError: ChatError) => {
      logChatError(chatError, { managerId: this.config?.userId });
      this.events.onMessageSendError?.(chatError);
    });
  }

  async connect(): Promise<void> {
    if (this.isDestroyed || !this.client) {
      throw new Error('Connection manager is destroyed or not initialized');
    }

    try {
      useConnectionStore.getState().setConnecting(true);
      useConnectionStore.getState().setConnectionStatus('CONNECTING');
      
      await this.client.connect();
      
    } catch (error) {
      useConnectionStore.getState().setConnecting(false);
      useConnectionStore.getState().setConnectionStatus('DISCONNECTED');
      throw error;
    } finally {
      useConnectionStore.getState().setConnecting(false);
    }
  }

  disconnect(): void {
    if (this.client) {
      this.client.disconnect();
      useConnectionStore.getState().setConnectionStatus('DISCONNECTED');
    }
  }

  sendMessage(content: string): void {
    if (!this.client || !this.client.getConnectionStatus()) {
      const connectionError = getErrorMessage({
        type: 'CONNECTION_ERROR',
        message: 'Not connected to chat server'
      });
      logChatError(connectionError, { content, managerId: this.config?.userId });
      throw connectionError;
    }
    
    try {
      this.client.sendChatMessage(content);
    } catch (error) {
      // SocketClient에서 이미 에러 처리되므로 다시 throw
      throw error;
    }
  }

  joinRoom(roomId: string, userData?: any): void {
    if (!this.client || !this.client.getConnectionStatus()) {
      const connectionError = getErrorMessage({
        type: 'CONNECTION_ERROR', 
        message: 'Not connected to chat server'
      });
      logChatError(connectionError, { roomId, managerId: this.config?.userId });
      throw connectionError;
    }
    
    this.client.joinRoom(roomId, userData);
  }

  leaveRoom(roomId: string): void {
    if (!this.client || !this.client.getConnectionStatus()) {
      const connectionError = getErrorMessage({
        type: 'CONNECTION_ERROR',
        message: 'Not connected to chat server' 
      });
      logChatError(connectionError, { roomId, managerId: this.config?.userId });
      throw connectionError;
    }
    
    this.client.leaveRoom(roomId);
  }

  getConnectionStatus(): boolean {
    return this.client?.getConnectionStatus() || false;
  }

  getConnectionState(): string {
    return this.client?.getConnectionState() || 'DISCONNECTED';
  }

  destroy(): void {
    this.isDestroyed = true;
    this.disconnect();
    useConnectionStore.getState().reset();
    this.client = null;
    this.config = null;
  }
}
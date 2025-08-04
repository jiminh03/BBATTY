// 추후 활용법
// const client = new SocketClient({
//   url: 'ws://localhost:8084/ws/game-chat',
//   options: {
//     auth: { sessionToken: 'your-token' },
//     query: { userId: '123', teamId: '1' }
//   }
// });

// await client.connect();
// client.sendChatMessage('안녕하세요!');

import { SocketConfig, WebSocketMessage } from './types';

class SocketClient {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 1000;
  private eventListeners: Map<string, ((data: any) => void)[]> = new Map();
  private isConnecting = false;
  private messageQueue: WebSocketMessage[] = [];
  private isAuthenticated = false;

  constructor(private config: SocketConfig) {}

  async connect(): Promise<WebSocket> {
    if (this.ws?.readyState === WebSocket.OPEN) {
      return Promise.resolve(this.ws);
    }

    if (this.isConnecting) {
      return new Promise((resolve, reject) => {
        const checkConnection = () => {
          if (this.ws?.readyState === WebSocket.OPEN) {
            resolve(this.ws);
          } else if (!this.isConnecting) {
            reject(new Error('Connection failed'));
          } else {
            setTimeout(checkConnection, 100);
          }
        };
        checkConnection();
      });
    }

    return new Promise((resolve, reject) => {
      try {
        this.isConnecting = true;
        
        let wsUrl = this.config.url;
        if (!wsUrl.startsWith('ws://') && !wsUrl.startsWith('wss://')) {
          wsUrl = wsUrl.replace(/^https?:\/\//, 'ws://');
        }
        
        const queryParams = new URLSearchParams();
        if (this.config.options?.query) {
          Object.entries(this.config.options.query).forEach(([key, value]) => {
            queryParams.append(key, String(value));
          });
        }
        
        if (this.config.options?.auth?.sessionToken) {
          queryParams.append('token', this.config.options.auth.sessionToken);
        }
        
        if (queryParams.toString()) {
          wsUrl += `?${queryParams.toString()}`;
        }

        this.ws = new WebSocket(wsUrl);

        this.ws.onopen = () => {
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          this.isAuthenticated = true;
          this.flushMessageQueue();
          this.emitEvent('connect');
          resolve(this.ws!);
        };

        this.ws.onclose = (event) => {
          this.isConnecting = false;
          this.isAuthenticated = false;
          this.emitEvent('disconnect', { code: event.code, reason: event.reason });
          this.handleReconnect();
        };

        this.ws.onerror = (error) => {
          this.isConnecting = false;
          this.isAuthenticated = false;
          this.emitEvent('connect_error', error);
          reject(error);
        };

        this.ws.onmessage = (event) => {
          try {
            const message: WebSocketMessage = JSON.parse(event.data);
            if (message.type) {
              this.emitEvent(message.type, message.payload || message);
            }
            this.emitEvent('message', message);
          } catch (error) {
            // JSON이 아닌 순수 문자열 메시지 처리
            this.emitEvent('message', { type: 'text', payload: event.data });
          }
        };

      } catch (error) {
        this.isConnecting = false;
        reject(error);
      }
    });
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect');
      this.ws = null;
    }
    this.isConnecting = false;
    this.isAuthenticated = false;
    this.messageQueue = [];
  }

  emit(event: string, data?: any): void {
    const message: WebSocketMessage = {
      type: event,
      payload: data,
      messageId: this.generateMessageId(),
      timestamp: new Date().toISOString()
    };

    if (this.ws?.readyState === WebSocket.OPEN && this.isAuthenticated) {
      this.ws.send(JSON.stringify(message));
    } else {
      this.messageQueue.push(message);
    }
  }

  sendChatMessage(content: string): void {
    if (this.ws?.readyState === WebSocket.OPEN && this.isAuthenticated) {
      this.ws.send(content);
    }
  }

  joinRoom(roomId: string, userData?: any): void {
    this.emit('join_room', { roomId, ...userData });
  }

  leaveRoom(roomId: string): void {
    this.emit('leave_room', { roomId });
  }

  private flushMessageQueue(): void {
    while (this.messageQueue.length > 0 && this.ws?.readyState === WebSocket.OPEN) {
      const message = this.messageQueue.shift();
      if (message) {
        this.ws.send(JSON.stringify(message));
      }
    }
  }

  private generateMessageId(): string {
    return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  on(event: string, callback: (data: any) => void): void {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, []);
    }
    this.eventListeners.get(event)!.push(callback);
  }

  off(event: string, callback?: (data: any) => void): void {
    if (!this.eventListeners.has(event)) return;
    
    if (callback) {
      const listeners = this.eventListeners.get(event)!;
      const index = listeners.indexOf(callback);
      if (index > -1) {
        listeners.splice(index, 1);
      }
    } else {
      this.eventListeners.delete(event);
    }
  }

  private emitEvent(event: string, data?: any): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      listeners.forEach(callback => {
        try {
          callback(data);
        } catch (error) {
          // 에러 무시
        }
      });
    }
  }

  private handleReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectInterval * Math.pow(2, this.reconnectAttempts - 1);
      
      setTimeout(() => {
        this.connect().catch(() => {});
      }, delay);
    } else {
      this.emitEvent('max_reconnect_failed');
    }
  }

  getConnectionStatus(): boolean {
    return this.ws?.readyState === WebSocket.OPEN && this.isAuthenticated;
  }

  getConnectionState(): string {
    if (!this.ws) return 'DISCONNECTED';
    
    switch (this.ws.readyState) {
      case WebSocket.CONNECTING: return 'CONNECTING';
      case WebSocket.OPEN: return this.isAuthenticated ? 'CONNECTED' : 'AUTHENTICATING';
      case WebSocket.CLOSING: return 'CLOSING';
      case WebSocket.CLOSED: return 'CLOSED';
      default: return 'UNKNOWN';
    }
  }
}

export { SocketClient };
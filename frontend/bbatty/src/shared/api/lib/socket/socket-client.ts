import { SocketConfig, WebSocketMessage } from './types';
import { getErrorMessage, logChatError, calculateRetryDelay, canRetry, ChatError } from '../../../utils/error';

class SocketClient {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 1000;
  private eventListeners: Map<string, ((data: any) => void)[]> = new Map();
  private isConnecting = false;
  private messageQueue: WebSocketMessage[] = [];
  private isAuthenticated = false;
  private currentError: ChatError | null = null;
  private connectionTimeoutId: NodeJS.Timeout | null = null;

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
            reject(this.currentError || new Error('Connection failed'));
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
        this.currentError = null;
        
        // 연결 타임아웃 설정 (15초)
        this.connectionTimeoutId = setTimeout(() => {
          if (this.isConnecting) {
            const timeoutError = getErrorMessage({ 
              code: 'TIMEOUT', 
              message: 'Connection timeout' 
            });
            this.currentError = timeoutError;
            logChatError(timeoutError, { url: this.config.url });
            this.emitEvent('connect_error', timeoutError);
            reject(timeoutError);
            this.cleanup();
          }
        }, 15000);
        
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
          this.clearConnectionTimeout();
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          this.isAuthenticated = true;
          this.currentError = null;
          this.flushMessageQueue();
          this.emitEvent('connect');
          resolve(this.ws!);
        };

        this.ws.onclose = (event) => {
          this.clearConnectionTimeout();
          this.isConnecting = false;
          this.isAuthenticated = false;
          
          const closeError = getErrorMessage({
            type: 'close',
            code: event.code,
            reason: event.reason
          });
          this.currentError = closeError;
          logChatError(closeError, { code: event.code, reason: event.reason });
          
          this.emitEvent('disconnect', closeError);
          this.handleReconnect(closeError);
        };

        this.ws.onerror = (error) => {
          this.clearConnectionTimeout();
          this.isConnecting = false;
          this.isAuthenticated = false;
          
          const connectionError = getErrorMessage({
            type: 'CONNECTION_ERROR',
            message: 'WebSocket connection error'
          });
          this.currentError = connectionError;
          logChatError(connectionError, { originalError: error });
          
          this.emitEvent('connect_error', connectionError);
          reject(connectionError);
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
        this.clearConnectionTimeout();
        this.isConnecting = false;
        const unknownError = getErrorMessage(error);
        this.currentError = unknownError;
        logChatError(unknownError, { originalError: error });
        reject(unknownError);
      }
    });
  }

  disconnect(): void {
    this.clearConnectionTimeout();
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect');
      this.ws = null;
    }
    this.cleanup();
  }

  private clearConnectionTimeout(): void {
    if (this.connectionTimeoutId) {
      clearTimeout(this.connectionTimeoutId);
      this.connectionTimeoutId = null;
    }
  }

  private cleanup(): void {
    this.isConnecting = false;
    this.isAuthenticated = false;
    this.messageQueue = [];
    this.currentError = null;
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
      try {
        this.ws.send(content);
      } catch (error) {
        const sendError = getErrorMessage({
          type: 'MESSAGE_SEND',
          message: 'Failed to send message'
        });
        logChatError(sendError, { content, originalError: error });
        this.emitEvent('message_send_error', sendError);
        throw sendError;
      }
    } else {
      const connectionError = getErrorMessage({
        type: 'CONNECTION_ERROR',
        message: 'Not connected to server'
      });
      logChatError(connectionError, { content });
      this.emitEvent('message_send_error', connectionError);
      throw connectionError;
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

  private handleReconnect(error?: ChatError): void {
    if (!error) {
      error = this.currentError || getErrorMessage({ message: 'Connection lost' });
    }
    
    if (canRetry(error, this.reconnectAttempts, this.maxReconnectAttempts)) {
      this.reconnectAttempts++;
      const delay = calculateRetryDelay(this.reconnectAttempts - 1, this.reconnectInterval);
      
      logChatError(error, { 
        reconnectAttempt: this.reconnectAttempts, 
        nextRetryIn: delay 
      });
      
      this.emitEvent('reconnecting', { 
        attempt: this.reconnectAttempts, 
        maxAttempts: this.maxReconnectAttempts,
        delay,
        error
      });
      
      setTimeout(() => {
        this.connect().catch((reconnectError) => {
          logChatError(getErrorMessage(reconnectError), { 
            isReconnectAttempt: true,
            attempt: this.reconnectAttempts 
          });
        });
      }, delay);
    } else {
      const maxReconnectError = getErrorMessage({
        message: 'Maximum reconnection attempts exceeded'
      });
      logChatError(maxReconnectError, { 
        totalAttempts: this.reconnectAttempts,
        lastError: error 
      });
      this.emitEvent('max_reconnect_failed', maxReconnectError);
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

  getCurrentError(): ChatError | null {
    return this.currentError;
  }

  getReconnectStatus(): { attempts: number; maxAttempts: number; canRetry: boolean } {
    const currentError = this.currentError || getErrorMessage({ message: 'No error' });
    return {
      attempts: this.reconnectAttempts,
      maxAttempts: this.maxReconnectAttempts,
      canRetry: canRetry(currentError, this.reconnectAttempts, this.maxReconnectAttempts)
    };
  }

  forceReconnect(): Promise<WebSocket> {
    this.disconnect();
    this.reconnectAttempts = 0;
    return this.connect();
  }
}

export { SocketClient };
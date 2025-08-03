import io, { Socket } from 'socket.io-client';
import { SocketConfig } from './types';

class SocketClient {
  private socket: Socket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 1000;

  constructor(private config: SocketConfig) {}

  connect(): Promise<Socket> {
    if (this.socket?.connected) {
      return Promise.resolve(this.socket);
    }

    return new Promise((resolve, reject) => {
      try {
        this.socket = io(this.config.url, {
          transports: ['websocket'],
          timeout: 20000,
          forceNew: true,
          ...this.config.options,
        });

        this.socket.on('connect', () => {
          console.log('Socket connected');
          this.reconnectAttempts = 0;
          resolve(this.socket!);
        });

        this.socket.on('disconnect', (reason) => {
          console.log('Socket disconnected:', reason);
          this.handleReconnect();
        });

        this.socket.on('connect_error', (error) => {
          console.error('Socket connection error:', error);
          reject(error);
        });

      } catch (error) {
        reject(error);
      }
    });
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
    }
  }

  emit(event: string, data?: any): void {
    if (this.socket?.connected) {
      this.socket.emit(event, data);
    } else {
      console.warn('Socket not connected, cannot emit:', event);
    }
  }

  on(event: string, callback: (data: any) => void): void {
    this.socket?.on(event, callback);
  }

  off(event: string, callback?: (data: any) => void): void {
    this.socket?.off(event, callback);
  }

  private handleReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      setTimeout(() => {
        this.connect().catch(console.error);
      }, this.reconnectInterval * this.reconnectAttempts);
    }
  }

  getConnectionStatus(): boolean {
    return this.socket?.connected ?? false;
  }
}

export { SocketClient };
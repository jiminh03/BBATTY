import { SocketClient } from './socket-client';
import { SocketConfig } from './types';

class SocketManager {
  private static instance: SocketManager;
  private client: SocketClient | null = null;

  private constructor() {}

  static getInstance(): SocketManager {
    if (!SocketManager.instance) {
      SocketManager.instance = new SocketManager();
    }
    return SocketManager.instance;
  }

  async initialize(config: SocketConfig): Promise<void> {
    if (!this.client) {
      this.client = new SocketClient(config);
      await this.client.connect();
    }
  }

  getClient(): SocketClient | null {
    return this.client;
  }

  disconnect(): void {
    this.client?.disconnect();
    this.client = null;
  }
}

export const socketManager = SocketManager.getInstance();
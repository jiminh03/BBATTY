import { useEffect, useRef, useCallback } from 'react';
import { ChatConnectionManager } from '../lib/chatConnection';
import { useConnectionStore } from '../model/store';
import { ConnectionConfig, ConnectionEvents } from '../model/types';
import { useCreateChatSession } from '../../../entities/chat-room';

interface UseChatConnectionOptions extends ConnectionEvents {
  autoConnect?: boolean;
  retryOnFailure?: boolean;
}

export const useChatConnection = (
  config: Omit<ConnectionConfig, 'sessionToken'> & { accessToken: string; chatType: string },
  options: UseChatConnectionOptions = {}
) => {
  const connectionManager = useRef<ChatConnectionManager | null>(null);
  const { 
    client, 
    isConnecting, 
    isConnected, 
    connectionStatus, 
    error,
    reconnectAttempts 
  } = useConnectionStore();

  const createSessionMutation = useCreateChatSession();

  const connect = useCallback(async () => {
    try {
      // 1. 세션 토큰 발급
      const sessionResponse = await createSessionMutation.mutateAsync({
        accessToken: config.accessToken,
        chatType: config.chatType,
        roomId: config.roomId,
      });

      if (!sessionResponse.success || !sessionResponse.sessionToken) {
        throw new Error(sessionResponse.errorMessage || 'Failed to create session');
      }

      // 2. 연결 매니저 초기화
      const connectionConfig: ConnectionConfig = {
        ...config,
        sessionToken: sessionResponse.sessionToken,
        url: sessionResponse.websocketUrl,
      };

      connectionManager.current = new ChatConnectionManager(connectionConfig, {
        onConnect: options.onConnect,
        onDisconnect: options.onDisconnect,
        onError: options.onError,
        onMessage: options.onMessage,
        onReconnect: options.onReconnect,
        onMaxReconnectFailed: options.onMaxReconnectFailed,
      });

      // 3. 연결 시도
      await connectionManager.current.connect();

    } catch (error) {
      console.error('Chat connection failed:', error);
      throw error;
    }
  }, [config, options, createSessionMutation]);

  const disconnect = useCallback(() => {
    connectionManager.current?.disconnect();
  }, []);

  const sendMessage = useCallback((content: string) => {
    if (!connectionManager.current) {
      throw new Error('Connection not initialized');
    }
    connectionManager.current.sendMessage(content);
  }, []);

  const joinRoom = useCallback((roomId: string, userData?: any) => {
    if (!connectionManager.current) {
      throw new Error('Connection not initialized');
    }
    connectionManager.current.joinRoom(roomId, userData);
  }, []);

  const leaveRoom = useCallback((roomId: string) => {
    if (!connectionManager.current) {
      throw new Error('Connection not initialized');
    }
    connectionManager.current.leaveRoom(roomId);
  }, []);

  // 자동 연결
  useEffect(() => {
    if (options.autoConnect && !isConnected && !isConnecting) {
      connect().catch(console.error);
    }
  }, [options.autoConnect, isConnected, isConnecting, connect]);

  // 정리
  useEffect(() => {
    return () => {
      connectionManager.current?.destroy();
    };
  }, []);

  return {
    // 상태
    isConnecting,
    isConnected,
    connectionStatus,
    error,
    reconnectAttempts,
    
    // 액션
    connect,
    disconnect,
    sendMessage,
    joinRoom,
    leaveRoom,
    
    // 유틸리티
    getConnectionStatus: () => connectionManager.current?.getConnectionStatus() || false,
    getConnectionState: () => connectionManager.current?.getConnectionState() || 'DISCONNECTED',
  };
};
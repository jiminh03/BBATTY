import { useEffect, useRef, useCallback } from 'react';
import { ChatConnectionManager } from '../lib/chatConnection';
import { useConnectionStore } from '../model/store';
import { ConnectionConfig, ConnectionEvents } from '../model/types';
import { useCreateChatSession } from '../../../entities/chat-room';
import type { AuthRequest, MatchChatJoinRequest, WatchChatJoinRequest } from '../../../entities/chat-room';

interface UseChatConnectionOptions extends ConnectionEvents {
  autoConnect?: boolean;
  retryOnFailure?: boolean;
}

export const useChatConnection = (
  chatType: 'MATCH' | 'WATCH',
  config: AuthRequest,
  options: UseChatConnectionOptions = {}
) => {
  const connectionManager = useRef<ChatConnectionManager | null>(null);
  const { 
    isConnecting, 
    isConnected, 
    connectionStatus, 
    error,
    reconnectAttempts 
  } = useConnectionStore();

  const createSessionMutation = useCreateChatSession(chatType);

  const connect = useCallback(async () => {
    try {
      // 1. 세션 토큰 발급 (chatType에 따라 다른 API 호출)
      const sessionResponse = await createSessionMutation.mutateAsync(config as any);

      if (!sessionResponse.success || !sessionResponse.sessionToken) {
        throw new Error(sessionResponse.errorMessage || 'Failed to create session');
      }

      // 2. 연결 매니저 초기화
      const roomId = chatType === 'MATCH' 
        ? (config as MatchChatJoinRequest).matchId 
        : (config as WatchChatJoinRequest).gameId;

      const connectionConfig: ConnectionConfig = {
        url: sessionResponse.websocketUrl,
        sessionToken: sessionResponse.sessionToken,
        roomId: roomId,
        // API 요청 본문에는 없지만, 웹소켓 연결 시 필요한 정보는 이전처럼 config에서 가져옵니다.
        userId: (config as any).userId || '',
        teamId: (config as any).teamId,
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

    } catch (err) {
      console.error('Chat connection failed:', err);
      throw err;
    }
  }, [chatType, config, options, createSessionMutation]);

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
    isConnecting,
    isConnected,
    connectionStatus,
    error,
    reconnectAttempts,
    connect,
    disconnect,
    sendMessage,
    joinRoom,
    leaveRoom,
    getConnectionStatus: () => connectionManager.current?.getConnectionStatus() || false,
    getConnectionState: () => connectionManager.current?.getConnectionState() || 'DISCONNECTED',
  };
};
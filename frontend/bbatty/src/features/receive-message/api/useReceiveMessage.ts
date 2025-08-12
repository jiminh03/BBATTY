import { SocketClient } from '../../../shared/api/lib/socket/socket-client';
import { useEffect, useCallback, useRef } from 'react';
import { useReceiveMessageStore } from '../model/store';
import { ChatMessage, LoadMoreMessagesRequest, LoadMoreMessagesResponse } from '../model/types';
import { useChatRoomStore } from '../../../entities/chat-room';
import { useConnectionStore } from '../../chat-connection';


interface UseReceiveMessageReturn {
  messages: ChatMessage[];
  isLoading: boolean;
  hasMore: boolean;
  connectionError: string | null;
  loadMoreMessages: () => Promise<void>;
  clearMessages: () => void;
}

export const useReceiveMessage = (roomId: string): UseReceiveMessageReturn => {
  const {
    getMessagesByRoom,
    addMessage,
    addMessages,
    clearMessages: clearRoomMessages,
    isLoadingMessages,
    hasMoreMessagesForRoom,
    getLastMessageTimestamp,
    setLoadingMessages,
    setHasMoreMessages,
    connectionError,
    setConnectionError,
  } = useReceiveMessageStore();

  const { sessionToken, currentRoom } = useChatRoomStore();
  const { isConnected, client } = useConnectionStore();
  const socketClientRef = useRef<SocketClient | null>(null);
  const loadMoreTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // 메시지 리스너 설정
  const setupMessageListeners = useCallback((client: SocketClient) => {
    // 일반 메시지 수신
    client.on('message', (data: ChatMessage) => {
      if ('roomId' in data && data.roomId === roomId) {
        addMessage(roomId, data);
      }
    });

    // 히스토리 메시지 응답 수신
    client.on('LOAD_MORE_MESSAGES_RESPONSE', (response: LoadMoreMessagesResponse) => {
      if (response.matchId === roomId) {
        setLoadingMessages(false);
        if (response.messages && response.messages.length > 0) {
          // 이전 메시지들을 앞에 추가 (prepend = true)
          addMessages(roomId, response.messages, true);
        }
        setHasMoreMessages(roomId, response.hasMore);
      }
    });

    // 연결 에러
    client.on('connect_error', (error: any) => {
      setConnectionError(error?.message || '연결 오류가 발생했습니다.');
      setLoadingMessages(false);
    });

    // 연결 해제
    client.on('disconnect', () => {
      setConnectionError('연결이 해제되었습니다.');
    });

    // 연결 성공
    client.on('connect', () => {
      setConnectionError(null);
    });
  }, [roomId, addMessage, addMessages, setLoadingMessages, setHasMoreMessages, setConnectionError]);

  // WebSocket 클라이언트 초기화
  useEffect(() => {
    // 이미 연결된 클라이언트가 있다면 그것을 사용
    if (client && isConnected) {
      socketClientRef.current = client;
      setupMessageListeners(client);
      setConnectionError(null);
      return;
    }

    if (!isConnected || !sessionToken || !currentRoom) {
      return;
    }

    // 기존 클라이언트 정리
    if (socketClientRef.current) {
      socketClientRef.current.disconnect();
    }

    try {
      // 채팅 타입에 따른 WebSocket URL 결정
      const wsUrl = currentRoom.type === 'game' 
        ? 'ws://i13a403.p.ssafy.io:8084/ws/game-chat'
        : 'ws://i13a403.p.ssafy.io:8083/ws/match-chat';

      // 게임 채팅의 경우 추가 파라미터 필요
      const queryParams = currentRoom.type === 'game' 
        ? { teamId: (currentRoom as any).teamId }
        : {};

      const client = new SocketClient({
        url: wsUrl,
        options: {
          auth: { sessionToken },
          query: queryParams
        }
      });

      setupMessageListeners(client);
      
      client.connect()
        .then(() => {
          socketClientRef.current = client;
          setConnectionError(null);
        })
        .catch((error) => {
          setConnectionError(error?.message || 'WebSocket 연결에 실패했습니다.');
        });

    } catch (error) {
      setConnectionError('WebSocket 클라이언트 초기화에 실패했습니다.');
    }

    return () => {
      if (socketClientRef.current) {
        socketClientRef.current.disconnect();
        socketClientRef.current = null;
      }
      if (loadMoreTimeoutRef.current) {
        clearTimeout(loadMoreTimeoutRef.current);
      }
    };
  }, [isConnected, sessionToken, currentRoom, setupMessageListeners, setConnectionError]);

  // 더 많은 메시지 로드
  const loadMoreMessages = useCallback(async (): Promise<void> => {
    if (isLoadingMessages || !hasMoreMessagesForRoom(roomId)) {
      return;
    }

    // 현재 사용 중인 클라이언트 확인 (기존 클라이언트 우선 사용)
    const currentClient = client || socketClientRef.current;
    if (!currentClient) {
      return;
    }

    const lastTimestamp = getLastMessageTimestamp(roomId);
    if (!lastTimestamp) {
      return;
    }

    setLoadingMessages(true);

    try {
      const request: LoadMoreMessagesRequest = {
        type: 'LOAD_MORE_MESSAGES',
        matchId: roomId,
        lastMessageTimestamp: lastTimestamp,
        limit: 50
      };

      // WebSocket으로 히스토리 요청
      currentClient.emit('LOAD_MORE_MESSAGES', request);

      // 타임아웃 설정 (10초)
      loadMoreTimeoutRef.current = setTimeout(() => {
        setLoadingMessages(false);
        setConnectionError('메시지 로드 시간이 초과되었습니다.');
      }, 10000);

    } catch (error) {
      setLoadingMessages(false);
      setConnectionError('메시지 로드 중 오류가 발생했습니다.');
    }
  }, [
    isLoadingMessages,
    hasMoreMessagesForRoom,
    roomId,
    getLastMessageTimestamp,
    setLoadingMessages,
    setConnectionError
  ]);

  // 메시지 정리
  const clearMessages = useCallback(() => {
    clearRoomMessages(roomId);
  }, [clearRoomMessages, roomId]);

  // 컴포넌트 언마운트 시 정리
  useEffect(() => {
    return () => {
      if (loadMoreTimeoutRef.current) {
        clearTimeout(loadMoreTimeoutRef.current);
      }
    };
  }, []);

  return {
    messages: getMessagesByRoom(roomId),
    isLoading: isLoadingMessages,
    hasMore: hasMoreMessagesForRoom(roomId),
    connectionError,
    loadMoreMessages,
    clearMessages,
  };
};
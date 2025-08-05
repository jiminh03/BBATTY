import { useEffect, useCallback, useRef } from 'react';
import { useReceiveMessageStore } from '../model/store';
import { ChatMessage } from '../model/types';
import { generateInitialMessages, generateHistoryMessages, generateNewMessage } from '../utils/mockData';

interface UseMockReceiveMessageReturn {
  messages: ChatMessage[];
  isLoading: boolean;
  hasMore: boolean;
  connectionError: string | null;
  loadMoreMessages: () => Promise<void>;
  clearMessages: () => void;
  startRealTimeSimulation: () => void;
  stopRealTimeSimulation: () => void;
}

export const useMockReceiveMessage = (
  roomId: string,
  chatType: 'match' | 'game' = 'match'
): UseMockReceiveMessageReturn => {
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

  const realTimeIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const hasInitialized = useRef(false);

  // 초기 메시지 로드
  useEffect(() => {
    if (!hasInitialized.current) {
      hasInitialized.current = true;
      
      // 초기 메시지 생성 및 추가
      const initialMessages = generateInitialMessages(30, chatType);
      addMessages(roomId, initialMessages);
      
      // 더 많은 메시지가 있다고 설정
      setHasMoreMessages(roomId, true);
      setConnectionError(null);
    }
  }, [roomId, chatType, addMessages, setHasMoreMessages, setConnectionError]);

  // 더 많은 메시지 로드 (목업)
  const loadMoreMessages = useCallback(async (): Promise<void> => {
    if (isLoadingMessages || !hasMoreMessagesForRoom(roomId)) {
      return;
    }

    const lastTimestamp = getLastMessageTimestamp(roomId);
    if (!lastTimestamp) {
      return;
    }

    setLoadingMessages(true);
    setConnectionError(null);

    // 네트워크 지연 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 1000));

    try {
      // 히스토리 메시지 생성
      const historyMessages = generateHistoryMessages(lastTimestamp, 20, chatType);
      
      if (historyMessages.length > 0) {
        // prepend = true로 이전 메시지들을 앞에 추가
        addMessages(roomId, historyMessages, true);
      }
      
      // 랜덤하게 더 이상 메시지가 없다고 설정 (30% 확률)
      if (Math.random() < 0.3) {
        setHasMoreMessages(roomId, false);
      }
      
    } catch (error) {
      setConnectionError('메시지 로드 중 오류가 발생했습니다.');
    } finally {
      setLoadingMessages(false);
    }
  }, [
    isLoadingMessages,
    hasMoreMessagesForRoom,
    roomId,
    chatType,
    getLastMessageTimestamp,
    setLoadingMessages,
    addMessages,
    setHasMoreMessages,
    setConnectionError
  ]);

  // 실시간 메시지 시뮬레이션 시작
  const startRealTimeSimulation = useCallback(() => {
    if (realTimeIntervalRef.current) {
      clearInterval(realTimeIntervalRef.current);
    }

    realTimeIntervalRef.current = setInterval(() => {
      // 30% 확률로 새 메시지 생성
      if (Math.random() < 0.3) {
        const newMessage = generateNewMessage(chatType);
        // roomId 설정
        if ('roomId' in newMessage) {
          (newMessage as any).roomId = roomId;
        }
        addMessage(roomId, newMessage);
      }
    }, 3000 + Math.random() * 5000); // 3-8초마다 메시지 생성
  }, [roomId, chatType, addMessage]);

  // 실시간 메시지 시뮬레이션 중지
  const stopRealTimeSimulation = useCallback(() => {
    if (realTimeIntervalRef.current) {
      clearInterval(realTimeIntervalRef.current);
      realTimeIntervalRef.current = null;
    }
  }, []);

  // 메시지 정리
  const clearMessages = useCallback(() => {
    clearRoomMessages(roomId);
    hasInitialized.current = false;
  }, [clearRoomMessages, roomId]);

  // 컴포넌트 언마운트 시 정리
  useEffect(() => {
    return () => {
      stopRealTimeSimulation();
    };
  }, [stopRealTimeSimulation]);

  return {
    messages: getMessagesByRoom(roomId),
    isLoading: isLoadingMessages,
    hasMore: hasMoreMessagesForRoom(roomId),
    connectionError,
    loadMoreMessages,
    clearMessages,
    startRealTimeSimulation,
    stopRealTimeSimulation,
  };
};
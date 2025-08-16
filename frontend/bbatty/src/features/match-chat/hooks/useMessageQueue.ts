import { useState, useCallback, useRef, useEffect } from 'react';
import type { PendingMessage, MessageStatus } from '../types';
import { getErrorMessage, calculateRetryDelay, canRetry, logChatError } from '../../../shared/utils/error';

interface UseMessageQueueProps {
  onSendMessage: (content: string) => Promise<boolean>;
  isConnected: boolean;
  maxRetries?: number;
}

export const useMessageQueue = ({
  onSendMessage,
  isConnected,
  maxRetries = 3,
}: UseMessageQueueProps) => {
  const [pendingMessages, setPendingMessages] = useState<PendingMessage[]>([]);
  const retryTimeouts = useRef<Map<string, NodeJS.Timeout>>(new Map());

  // 메시지를 큐에 추가하고 전송 시도
  const addMessageToQueue = useCallback(async (content: string): Promise<string> => {
    const messageId = `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const timestamp = new Date().toISOString();
    
    const pendingMessage: PendingMessage = {
      id: messageId,
      content,
      timestamp,
      status: 'sending',
      retryCount: 0,
      maxRetries,
      originalMessage: content,
    };

    // 큐에 메시지 추가
    setPendingMessages(prev => [...prev, pendingMessage]);

    // 즉시 전송 시도
    await attemptSendMessage(pendingMessage);
    
    return messageId;
  }, [onSendMessage, isConnected, maxRetries]);

  // 메시지 전송 시도
  const attemptSendMessage = useCallback(async (message: PendingMessage) => {
    if (!isConnected) {
      // 연결되지 않은 상태에서는 대기 상태로 유지 (재연결 시 자동 재시도)
      console.log(`📤 연결 대기 중: ${message.content.substring(0, 30)}...`);
      updateMessageStatus(message.id, 'sending');
      return;
    }

    updateMessageStatus(message.id, message.retryCount > 0 ? 'retrying' : 'sending');

    try {
      const success = await onSendMessage(message.content);
      
      if (success) {
        // 전송 성공 - 즉시 큐에서 제거 (웹소켓으로 받은 메시지가 표시됨)
        removeMessage(message.id);
      } else {
        // 전송 실패 - 재시도 스케줄링
        await handleMessageFailure(message);
      }
    } catch (error) {
      console.error('메시지 전송 오류:', error);
      await handleMessageFailure(message);
    }
  }, [isConnected, onSendMessage]);

  // 메시지 전송 실패 처리
  const handleMessageFailure = useCallback(async (message: PendingMessage) => {
    const chatError = getErrorMessage({
      type: 'MESSAGE_SEND',
      message: '메시지 전송 실패'
    });

    logChatError(chatError, { messageId: message.id, content: message.content });

    const newRetryCount = message.retryCount + 1;
    
    if (canRetry(chatError, newRetryCount, maxRetries)) {
      // 재시도 가능한 경우
      const updatedMessage = {
        ...message,
        retryCount: newRetryCount,
        status: 'failed' as MessageStatus,
      };

      updateMessageInQueue(message.id, updatedMessage);

      // 지수 백오프로 재시도 스케줄링
      const delay = calculateRetryDelay(newRetryCount - 1);
      
      const timeoutId = setTimeout(() => {
        retryTimeouts.current.delete(message.id);
        attemptSendMessage(updatedMessage);
      }, delay);

      retryTimeouts.current.set(message.id, timeoutId);
    } else {
      // 재시도 한도 초과
      updateMessageStatus(message.id, 'failed');
    }
  }, [maxRetries]);

  // 메시지 상태 업데이트
  const updateMessageStatus = useCallback((messageId: string, status: MessageStatus) => {
    setPendingMessages(prev =>
      prev.map(msg =>
        msg.id === messageId ? { ...msg, status } : msg
      )
    );
  }, []);

  // 큐에서 메시지 업데이트
  const updateMessageInQueue = useCallback((messageId: string, updatedMessage: PendingMessage) => {
    setPendingMessages(prev =>
      prev.map(msg =>
        msg.id === messageId ? updatedMessage : msg
      )
    );
  }, []);

  // 큐에서 메시지 제거
  const removeMessage = useCallback((messageId: string) => {
    // 타이머 정리
    const timeoutId = retryTimeouts.current.get(messageId);
    if (timeoutId) {
      clearTimeout(timeoutId);
      retryTimeouts.current.delete(messageId);
    }

    setPendingMessages(prev => prev.filter(msg => msg.id !== messageId));
  }, []);

  // 수동 재시도
  const retryMessage = useCallback(async (messageId: string) => {
    const message = pendingMessages.find(msg => msg.id === messageId);
    if (!message) return;

    // 기존 타이머 취소
    const timeoutId = retryTimeouts.current.get(messageId);
    if (timeoutId) {
      clearTimeout(timeoutId);
      retryTimeouts.current.delete(messageId);
    }

    await attemptSendMessage(message);
  }, [pendingMessages, attemptSendMessage]);

  // 연결 복구 시 실패한 메시지들 재전송
  const retryAllFailedMessages = useCallback(async () => {
    if (!isConnected) return;

    const failedMessages = pendingMessages.filter(msg => msg.status === 'failed');
    
    for (const message of failedMessages) {
      if (message.retryCount < maxRetries) {
        await attemptSendMessage(message);
        // 메시지 간 간격을 두어 서버 부하 방지
        await new Promise(resolve => setTimeout(resolve, 500));
      }
    }
  }, [isConnected, pendingMessages, maxRetries, attemptSendMessage]);

  // 모든 메시지 재시도 (연결 복구 시 자동 호출)
  const flushQueue = useCallback(async () => {
    await retryAllFailedMessages();
  }, [retryAllFailedMessages]);

  // 연결 상태 변경 시 대기 중인 메시지들 재시도
  useEffect(() => {
    if (isConnected) {
      console.log('🔄 연결 복구됨 - 대기 중인 메시지들 재시도');
      const waitingMessages = pendingMessages.filter(msg => 
        msg.status === 'sending' || msg.status === 'failed'
      );
      
      if (waitingMessages.length > 0) {
        console.log(`📤 ${waitingMessages.length}개 메시지 재시도 중...`);
        setTimeout(() => {
          retryAllFailedMessages();
        }, 1000); // 연결 안정화를 위해 1초 대기
      }
    }
  }, [isConnected, pendingMessages, retryAllFailedMessages]);

  return {
    pendingMessages,
    addMessageToQueue,
    retryMessage,
    removeMessage,
    flushQueue,
    retryAllFailedMessages,
  };
};
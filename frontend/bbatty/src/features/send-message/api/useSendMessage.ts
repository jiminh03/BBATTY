import { useMutation } from '@tanstack/react-query';
import { sendMessageApi } from './sendMessageApi';
import { useSendMessageStore } from '../model/store';
import { useConnectionStatus } from '../../chat-connection';
import { SendMessageRequest, SendMessageOptions } from '../model/types';
import { getErrorMessage, logChatError, ChatError } from '../../../shared/utils/error';

interface UseSendMessageOptions {
  onSuccess?: (message: string) => void;
  onError?: (error: ChatError) => void;
}

export const useSendMessage = (options: UseSendMessageOptions = {}) => {
  const { 
    setLoading, 
    setError, 
    setSentMessage, 
    incrementSendCount,
    clearMessage 
  } = useSendMessageStore();
  
  const { canSendMessage } = useConnectionStatus();

  return useMutation<void, ChatError, SendMessageRequest>({
    mutationFn: async (request) => {
      // 연결 상태 체크
      if (!canSendMessage()) {
        const connectionError = getErrorMessage({
          type: 'CONNECTION_ERROR',
          message: 'Not connected to chat server'
        });
        logChatError(connectionError, { request });
        throw connectionError;
      }

      setLoading(true);
      setError(null);

      try {
        await sendMessageApi.sendChatMessage(request);
        setSentMessage(request.content);
        incrementSendCount();
        options.onSuccess?.(request.content);
      } catch (error) {
        const chatError = error instanceof Object && 'type' in error 
          ? error as ChatError 
          : getErrorMessage(error);
        
        logChatError(chatError, { request });
        setError(chatError.userMessage);
        options.onError?.(chatError);
        throw chatError;
      } finally {
        setLoading(false);
      }
    },
  });
};
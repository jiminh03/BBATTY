import { useMutation } from '@tanstack/react-query';
import { sendMessageApi } from './sendMessageApi';
import { useSendMessageStore } from '../model/store';
import { useConnectionStatus } from '../../chat-connection';
import { SendMessageRequest, SendMessageOptions } from '../model/types';

interface UseSendMessageOptions {
  onSuccess?: (message: string) => void;
  onError?: (error: Error) => void;
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

  return useMutation<void, Error, SendMessageRequest>({
    mutationFn: async (request) => {
      // 연결 상태 체크
      if (!canSendMessage()) {
        throw new Error('채팅 서버에 연결되지 않았습니다.');
      }

      setLoading(true);
      setError(null);

      try {
        await sendMessageApi.sendChatMessage(request);
        setSentMessage(request.content);
        incrementSendCount();
        options.onSuccess?.(request.content);
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : '메시지 전송에 실패했습니다.';
        setError(errorMessage);
        options.onError?.(new Error(errorMessage));
        throw error;
      } finally {
        setLoading(false);
      }
    },
  });
};
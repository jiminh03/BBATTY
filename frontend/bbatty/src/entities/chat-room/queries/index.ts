import { useApiMutation, useApi } from '../../../shared/api/hooks/useApi';
import { QueryKeys } from '../../../shared/api/lib/tanstack/queryKeys';
import { chatRoomApi } from '../api';
import { AuthRequest, AuthResponse } from '../model/types';

export const useChatRoomSession = () => {
  return useApiMutation<AuthResponse, Error, AuthRequest>({
    apiFunction: (request: AuthRequest) => chatRoomApi.createSession(request),
    onSuccess: (data) => {
      console.log('Chat session created:', data);
    },
    onError: (error) => {
      console.error('Failed to create chat session:', error);
    }
  });
};

export const useInvalidateSession = () => {
  return useApiMutation<void, Error, string>({
    apiFunction: (sessionToken: string) => chatRoomApi.invalidateSession(sessionToken),
    onSuccess: () => {
      console.log('Session invalidated');
    },
    onError: (error) => {
      console.error('Failed to invalidate session:', error);
    }
  });
};

export const useConnectionInfo = () => {
  return useApi({
    queryKey: QueryKeys.entity('chat-connection-info'),
    apiFunction: () => chatRoomApi.getConnectionInfo(),
    staleTime: 5 * 60 * 1000, // 5분
  });
};

export const useChatStats = () => {
  return useApi({
    queryKey: QueryKeys.entity('chat-stats'),
    apiFunction: () => chatRoomApi.getChatStats(),
    refetchInterval: 30000, // 30초마다 자동 갱신
  });
};

export const useHealthCheck = () => {
  return useApi({
    queryKey: QueryKeys.entity('chat-health'),
    apiFunction: () => chatRoomApi.healthCheck(),
    staleTime: 60 * 1000, // 1분
  });
};
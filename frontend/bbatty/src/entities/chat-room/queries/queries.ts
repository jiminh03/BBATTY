import { useApiMutation } from '../../../shared/api';
import { chatRoomApi } from '../api/api';
import { AuthRequest, AuthResponse, MatchChatJoinRequest, WatchChatJoinRequest } from '../api/types';

export const useJoinMatchChat = () => {
  return useApiMutation<AuthResponse, Error, MatchChatJoinRequest>({
    apiFunction: chatRoomApi.joinMatchChat,
  });
};

export const useJoinWatchChat = () => {
  return useApiMutation<AuthResponse, Error, WatchChatJoinRequest>({
    apiFunction: chatRoomApi.joinWatchChat,
  });
};

export const useCreateChatSession = (chatType: 'MATCH' | 'WATCH') => {
  const joinMatchChat = useJoinMatchChat();
  const joinWatchChat = useJoinWatchChat();

  if (chatType === 'MATCH') {
    return joinMatchChat;
  }

  return joinWatchChat;
};

export const useInvalidateSession = () => {
  return useApiMutation<void, Error, string>({
    apiFunction: chatRoomApi.invalidateSession,
  });
};
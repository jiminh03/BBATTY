import { useApiMutation } from '../../../shared/api';
import { chatRoomApi } from '../api/api';
import { AuthRequest, AuthResponse } from '../api/types';

export const useCreateChatSession = () => {
  return useApiMutation<AuthResponse, Error, AuthRequest>({
    apiFunction: chatRoomApi.createChatSession,
  });
};

export const useInvalidateSession = () => {
  return useApiMutation<void, Error, string>({
    apiFunction: chatRoomApi.invalidateSession,
  });
};
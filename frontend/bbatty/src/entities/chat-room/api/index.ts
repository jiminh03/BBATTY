import { apiClient } from '../../../shared/api/client/apiClient';
import { AuthRequest, AuthResponse } from '../model/types';

export const chatRoomApi = {
  createSession: (request: AuthRequest) => {
    return apiClient.post<AuthResponse>('/api/chat/auth/session', request);
  },

  invalidateSession: (sessionToken: string) => {
    return apiClient.delete<void>(`/api/chat/auth/session/${sessionToken}`);
  },

  getConnectionInfo: () => {
    return apiClient.get('/api/chat/game/connection-info');
  },

  getChatStats: () => {
    return apiClient.get('/api/chat/game/stats');
  },

  healthCheck: () => {
    return apiClient.get('/api/chat/auth/health');
  }
};
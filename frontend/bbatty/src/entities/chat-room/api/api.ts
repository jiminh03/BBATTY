import { apiClient } from '../../../shared/api';
import { 
  AuthResponse, 
  MatchChatJoinRequest, 
  WatchChatJoinRequest 
} from './types';

export const chatRoomApi = {
  // 매치 채팅 참여
  joinMatchChat: (request: MatchChatJoinRequest) =>
    apiClient.post<AuthResponse>('/api/match-chat/join', request),

  // 관전 채팅 참여
  joinWatchChat: (request: WatchChatJoinRequest) =>
    apiClient.post<AuthResponse>('/api/watch-chat/join', request),

  // 세션 무효화
  invalidateSession: (sessionToken: string) => apiClient.delete<void>(`/api/chat/auth/session/${sessionToken}`),

  // 헬스체크
  healthCheck: () => apiClient.get<string>('/api/chat/auth/health'),
};

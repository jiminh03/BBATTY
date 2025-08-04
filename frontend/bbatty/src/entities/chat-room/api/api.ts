import { apiClient } from '../../../shared/api';
import { AuthRequest, AuthResponse } from './types';

export const chatRoomApi = {
  // 채팅 인증 및 세션 생성
  createChatSession: (authRequest: AuthRequest) =>
    apiClient.post<AuthResponse>('/api/chat/auth/session', authRequest),

  // 세션 무효화
  invalidateSession: (sessionToken: string) =>
    apiClient.delete<void>(`/api/chat/auth/session/${sessionToken}`),

  // 게임 채팅 연결 정보 조회
  getGameChatConnectionInfo: () =>
    apiClient.get('/api/chat/game/connection-info'),

  // 게임 채팅 통계 조회
  getGameChatStats: () =>
    apiClient.get('/api/chat/game/stats'),

  // 헬스체크
  healthCheck: () =>
    apiClient.get<string>('/api/chat/auth/health'),
};
import { apiClient } from '../../../shared';
import { ApiResponse } from '../../../shared';
import {
  CheckNicknameRequest,
  CheckNicknameResponse,
  RegisterRequest,
  RegisterResponse,
  RefreshTokenResponse,
} from '../model/authTypes';

export const authApi = {
  checkNickname: (request: CheckNicknameRequest) =>
    apiClient.get<CheckNicknameResponse>('/api/auth/check-nickname', {
      params: request,
    }),

  signup: (request: RegisterRequest) => apiClient.post<RegisterResponse>('/api/auth/signup', request),

  // 토큰 갱신 (헤더에 refresh token 전달)
  refreshToken: (refreshToken: string) =>
    apiClient.post<RefreshTokenResponse>('/api/auth/refresh', null, {
      headers: {
        'X-Refresh-Token': refreshToken,
      } as any,
    }),
};

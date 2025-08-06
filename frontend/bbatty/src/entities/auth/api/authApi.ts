import { apiClient, uploadClient } from '../../../shared';
import { User } from '../../user/model/types';
import type {
  KakaoLoginRequest,
  LoginResponse,
  RegisterRequest,
  Token,
  CheckNicknameRequest,
  CheckNicknameResponse,
} from '../model/types';

// 사용자 API
export const authApi = {
  // 카카오 로그인
  async kakaoLogin(data: string) {
    return apiClient.post<LoginResponse>('/api/auth/signup', data);
  },

  // 현재 사용자 정보 조회
  async fetchCurrentUser() {
    return apiClient.get<User>('/auth/me');
  },

  //  이미지 업로드
  async uploadImage(formData: FormData) {
    return uploadClient.post<{ imageUrl: string }>('/users/me/image', formData);
  },

  // 회원가입
  async register(data: RegisterRequest) {
    return apiClient.post<Token>('/auth/register', data);
  },

  // 로그아웃
  async logout() {
    return apiClient.post<void>('/api/auth/logout');
  },

  // 토큰 갱신
  async refreshToken() {
    return apiClient.post<Token>('/api/auth/refresh');
  },

  // 닉네임 중복 확인
  async checkNickname(data: CheckNicknameRequest) {
    return apiClient.post<CheckNicknameResponse>('/auth/check-nickname', data);
  },
};

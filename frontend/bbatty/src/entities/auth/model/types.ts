import { User } from '../../user/model/types';

export interface Token {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: Date;
  refreshTokenExpiresAt: Date;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

export interface AuthActions {
  setUser: (user: User | null) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  kakaoLogin: (kakaoAccessToken: string) => Promise<LoginResponse>;
  logout: () => Promise<void | null>;
  register: (data: RegisterRequest) => Promise<void>;
  updateUser: (updates: Partial<User>) => void;
  clearAuth: () => void;
  // refreshAuth: () => Promise<void | null>;
}

// 인증 스토어 타입
export type AuthStore = AuthState & AuthActions;

// ================================== Request =======================================

export interface KakaoLoginRequest {
  accessToken: string;
}

export interface CheckNicknameRequest {
  nickname: string;
}

export interface RegisterRequest {
  accessToken: string;
  kakaoId: string;
  email: string;
  birthYear: string;
  gender: string;
  teamId: number;
  nickname: string;
  profileImg?: string;
  introduction?: string;
}

// ================================== Response =======================================

export interface LoginResponse {
  token: Token;
  user: User;
}

export interface CheckNicknameResponse {
  isAvailable: boolean;
  message: string;
}

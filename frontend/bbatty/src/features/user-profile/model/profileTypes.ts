import { User } from '../../../entities/user/model/types';

// 프로필 폼 데이터
export interface ProfileFormData {
  nickname: string;
  profileImage?: string;
  introduction?: string;
}
export interface TokenInfo {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
}

// ================================= Request =======================================

export interface CheckNicknameRequest {
  nickname: string;
}

export interface RegisterRequest {
  accessToken: string; // 카카오 액세스 토큰
  kakaoId: string; // 카카오 ID
  email?: string;
  birthYear?: string;
  gender?: string;
  teamId: number;
  nickname: string;
  profileImg?: string;
  introduction?: string;
}

// ================================= Response ======================================

// 카카오 로그인 응답 타입
export interface KakaoUserInfo {
  id: number;
  properties?: {
    nickname?: string;
    profile_image?: string;
    thumbnail_image?: string;
  };
  kakao_account?: {
    profile_nickname_needs_agreement?: boolean;
    profile_image_needs_agreement?: boolean;
    profile?: {
      nickname?: string;
      thumbnail_image_url?: string;
      profile_image_url?: string;
    };
    has_email?: boolean;
    email_needs_agreement?: boolean;
    is_email_valid?: boolean;
    is_email_verified?: boolean;
    email?: string;
    birthday?: string;
    birthyear?: string;
    gender?: string;
  };
}

export interface CheckNicknameResponse {
  available: boolean;
}

export interface RegisterResponse {
  tokens: TokenInfo;
  userProfile: User;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
}

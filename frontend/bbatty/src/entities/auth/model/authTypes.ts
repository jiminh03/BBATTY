import { UserProfile } from '../../user/model/types';

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
  data: boolean;
}

export interface RegisterResponse {
  tokens: {
    accessToken: string;
    refreshToken: string;
    accessTokenExpiresAt: string;
    refreshTokenExpiresAt: string;
  };
  userProfile: {
    userId: number;
    nickname: string;
    profileImg?: string;
    teamId: number;
    teamName: string;
    introduction?: string;
    age: number;
    gender: string;
  };
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
}

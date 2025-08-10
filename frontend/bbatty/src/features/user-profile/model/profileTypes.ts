import { User } from '../../../entities/user/model/userTypes';
import { Token } from '../../../shared/api/token/tokenTypes';

// 기존 타입들과 새로운 타입들 통합
export interface UserProfile extends User {
  introduction?: string;
  totalWinRate: number;
  isFollowing?: boolean;
  isOwner?: boolean;
  privacySettings?: UserPrivacySettings;
}

export interface UserPrivacySettings {
  allowViewPosts: boolean;
  allowViewStats: boolean;
  allowViewDirectViewHistory: boolean;
}

// 프로필 폼 데이터
export interface ProfileFormData {
  nickname: string;
  profileImage?: string;
  introduction?: string;
}
// ================================= Request =======================================

export interface CheckNicknameRequest {
  nickname: string;
}

export interface UpdateProfileRequest {
  nickname?: string;
  profileImg?: string;
  introduction?: string;
}

// ================================= Response ======================================

export interface CheckNicknameResponse {
  available: boolean;
}

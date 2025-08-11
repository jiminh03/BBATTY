import { User } from '../../../entities/user/model/userTypes';

export interface UserProfile extends User, UserPrivacySettings {
  introduction?: string;
}

export interface UserPrivacySettings {
  postsPublic: boolean;
  statsPublic: boolean;
  attendanceRecordsPublic: boolean;
}

// 프로필 폼 데이터
export interface ProfileFormData {
  nickname: string;
  profileImage?: string;
  introduction?: string;
}
// ================================= Request =======================================

export interface UpdateProfileRequest {
  nickname?: string;
  profileImg?: string;
  introduction?: string;
}

export interface CheckNicknameRequest {
  nickname: string;
}

// ================================= Response ======================================

export interface CheckNicknameResponse {
  available: boolean;
}

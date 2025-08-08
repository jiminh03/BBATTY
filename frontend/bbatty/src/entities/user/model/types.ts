export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';

export interface BaseUser {
  id: string;
  nickname: string;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ChatUser extends BaseUser {
  teamId: string;
  age?: number;
  gender?: string;
  profileImgUrl?: string;
  isVictoryFairy?: boolean;
  winRate?: number;
}

// WebSocket 세션 정보
export interface UserSessionInfo {
  userId: string;
  userName: string;
  roomId: string;
  additionalInfo: Record<string, any>;
}

// JWT에서 추출되는 사용자 정보
export interface JwtUserInfo {
  userId: string;
  teamId: string;
  age: number;
  gender: string;
}

// ===================================== 추가 ==========================================
import { BaseEntity } from '../../../shared';

export interface User extends BaseEntity {
  nickname: string;
  profileImageURL?: string;
  introduction?: string;
  age: number;
  gender: string;
  teamId: string;
}

export interface UserProfile {
  userId: number;
  nickname: string;
  profileImg?: string;
  teamId: number;
  teamName: string;
  introduction?: string;
  age: number;
  gender: string;
}

export interface DetailedUser extends User {
  // 통계 엔티티에서 가져와야함
}

export interface UserSummary {
  nickname: string;
  profileImage?: string;
  introduction?: string;
}

// ================================== RequestPayload =======================================
export interface UpdateProfilePayload {
  username: string;
  introduction?: string;
  profileImageURL?: string;
}

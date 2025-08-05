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
  teamId: string;
  nickname: string;
  introduction?: string;
  profileImageURL?: string;
  gender?: string;
}

export interface DetailedUser extends User {
  // 통계 엔티티에서 가져와야함
}

export interface UserSummary {
  id: string;
  nickName: string;
  profileImage?: string;
}

// ================================== RequestPayload =======================================
export interface UpdateProfilePayload {
  username: string;
  introduction?: string;
  profileImageURL?: string;
}

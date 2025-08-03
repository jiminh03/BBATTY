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

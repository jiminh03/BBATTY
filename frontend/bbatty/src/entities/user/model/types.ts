import { UserStatus } from '../../../shared/types/enums';

export interface User {
  userId: string;
  nickname: string;
  profileImgUrl?: string;
  winRate?: number;
  status: UserStatus;
  teamId?: string;
  userTeamId?: string;
}

export interface MatchChatJoinRequest {
  nickname: string;
  profileImageUrl?: string;
  winRate?: number;
  matchChatRoomId: string;
}

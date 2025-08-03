import { UserStatus, ChatUser, JwtUserInfo } from '../model/types';

export const userUtils = {
  // 사용자 상태 체크
  canSendMessage: (status: UserStatus): boolean => {
    return status === 'ACTIVE';
  },

  canReceiveNotification: (status: UserStatus): boolean => {
    return status === 'ACTIVE';
  },

  canParticipateInChat: (status: UserStatus): boolean => {
    return status !== 'BLOCKED';
  },

  // JWT 토큰에서 사용자 정보 추출
  extractUserInfoFromJWT: (token: string): JwtUserInfo | null => {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        userId: payload.userId,
        teamId: payload.teamId,
        age: payload.age,
        gender: payload.gender,
      };
    } catch (error) {
      console.error('JWT 파싱 실패:', error);
      return null;
    }
  },

  // 사용자 프로필 이미지 URL 생성
  getProfileImageUrl: (profileImgUrl?: string): string => {
    return profileImgUrl || '/images/default-profile.png';
  },

  // 승률 표시 포맷팅
  formatWinRate: (winRate?: number): string => {
    return winRate ? `${winRate}%` : '정보없음';
  },

  // 사용자 표시명 생성
  getDisplayName: (user: ChatUser): string => {
    return user.nickname || `사용자${user.id.slice(-4)}`;
  },
};
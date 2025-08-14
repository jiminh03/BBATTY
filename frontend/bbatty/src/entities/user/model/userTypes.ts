export interface User {
  userId: number;
  nickname: string;
  profileImg?: string;
  teamId: number;
  age: number;
  gender: string;
  winRate?: number;
  isVictoryFairy?: boolean;
}

export interface UserSummary {
  nickname: string;
  profileImage?: string;
  introduction?: string;
}

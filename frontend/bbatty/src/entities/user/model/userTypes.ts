export interface User {
  userId: number;
  nickname: string;
  profileImg?: string;
  teamId: number;
  age: number;
  gender: string;
}

export interface UserSummary {
  nickname: string;
  profileImage?: string;
  introduction?: string;
}

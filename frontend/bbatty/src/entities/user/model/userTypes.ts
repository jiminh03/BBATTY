export interface User {
  userId: number;
  nickname: string;
  profileImg?: string;
  teamId: number;
  // teamName: string;
  // introduction?: string;
  age: number;
  gender: string;
}

export interface UserSummary {
  nickname: string;
  profileImage?: string;
  introduction?: string;
}

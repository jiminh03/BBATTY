// entities/comment/model/types.ts
export interface Comment {
  id: number | string;
  content: string;
  createdAt: string;
  updatedAt: string;
  isDeleted: boolean;
  authorNickname: string;

  // 서버가 줄 수도, 안 줄 수도 있는 필드들
  isMine?: boolean;
  userId: number;
  authorId?: number | string;
  writerId?: number | string;
}
